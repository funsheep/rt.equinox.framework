/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.util.*;
import org.osgi.framework.ServiceEvent;

/**
 * A registered service.
 *
 * The framework returns a ServiceRegistration object when a
 * {@link BundleContext#registerService BundleContext.registerService}
 * method is successful. This object is for the private use of
 * the registering bundle and should not be shared with other bundles.
 * <p>The ServiceRegistration object may be used to update the properties
 * for the service or to unregister the service.
 *
 * <p>If the ServiceRegistration is garbage collected the framework may remove
 * the service. This implies that if a
 * bundle wants to keep its service registered, it should keep the
 * ServiceRegistration object referenced.
 */

public class ServiceRegistration implements org.osgi.framework.ServiceRegistration
{
    /** Reference to this registration. */
    protected ServiceReference reference;

    /** Internal framework object. */
    protected Framework framework;

    /** context which registered this service. */
    protected BundleContext context;

    /** bundle which registered this service. */
    protected Bundle bundle;

    /** list of contexts using the service.
     * Access to this should be protected by the registrationLock */
    protected Vector contextsUsing;

    /** service classes for this registration. */
    protected String[] clazzes;

    /** service object for this registration. */
    protected Object service;

    /** properties for this registration. */
    protected Properties properties;

    /** service id. */
    protected long serviceid;

    /** service ranking. */
    protected int serviceranking;

    /* internal object to use for synchronization */
    protected Object registrationLock = new Object();

    /** This boolean will be true when the service registration is unregistered */
    protected boolean unregistered;

    /**
     * Construct a ServiceRegistration and register the service
     * in the framework's service registry.
     *
     */
    protected ServiceRegistration(BundleContext context, String[] clazzes, Object service, Dictionary properties)
    {
        this.context = context;
        this.bundle = context.bundle;
        this.framework = context.framework;
        this.clazzes = clazzes;     /* must be set before calling createProperties. */
        this.service = service;
        this.contextsUsing = null;
        this.unregistered = false;
        this.reference = new ServiceReference(this);

        synchronized (framework.serviceRegistry)
        {
            serviceid = framework.getNextServiceId();   /* must be set before calling createProperties. */
            this.properties = createProperties(properties);   /* must be valid after unregister is called. */

            if (Debug.DEBUG && Debug.DEBUG_SERVICES)
            {
                Debug.println("registerService["+bundle+"]("+this+")");
            }

			framework.serviceRegistry.publishService(context,this);
        }

        /* must not hold the registrations lock when this event is published */
        framework.publishServiceEvent(ServiceEvent.REGISTERED, reference);
    }

    /**
     * Unregister the service.
     * Remove a service registration from the framework's service
     * registry.
     * All {@link ServiceReference} objects for this registration
     * can no longer be used to interact with the service.
     *
     * <p>The following steps are followed to unregister a service:
     * <ol>
     * <li>The service is removed from the framework's service
     * registry so that it may no longer be used.
     * {@link ServiceReference}s for the service may no longer be used
     * to get a service object for the service.
     * <li>A {@link ServiceEvent} of type {@link ServiceEvent#UNREGISTERING}
     * is synchronously sent so that bundles using this service
     * may release their use of the service.
     * <li>For each bundle whose use count for this service is greater
     * than zero:
     * <ol>
     * <li>The bundle's use count for this service is set to zero.
     * <li>If the service was registered with a {@link ServiceFactory},
     * the {@link ServiceFactory#ungetService ServiceFactory.ungetService} method
     * is called to release the service object for the bundle.
     * </ol>
     * </ol>
     *
     * @exception java.lang.IllegalStateException If
     * this ServiceRegistration has already been unregistered.
     * @see BundleContext#ungetService
     */
    public void unregister()
    {
        synchronized (registrationLock)
        {
            if (service == null)    /* in the process of unregisterING */
            {
                throw new IllegalStateException(Msg.formatter.getString("SERVICE_ALREADY_UNREGISTERED_EXCEPTION"));
            }

            /* remove this object from the service registry */
            if (Debug.DEBUG && Debug.DEBUG_SERVICES)
            {
                Debug.println("unregisterService["+bundle+"]("+this+")");
            }

            synchronized (framework.serviceRegistry)
            {
				framework.serviceRegistry.unpublishService(context,this);
            }

            service = null;     /* mark unregisterING */
        }

        /* must not hold the registrationLock when this event is published */
        framework.publishServiceEvent(ServiceEvent.UNREGISTERING, reference);

        /* we have published the ServiceEvent, now mark the service fully unregistered */
        unregistered = true;

        int size = 0;
        BundleContext[] users = null;

        synchronized (registrationLock)
        {
            if (contextsUsing != null)
            {
                size = contextsUsing.size();

                if (size > 0)
                {
                    if (Debug.DEBUG && Debug.DEBUG_SERVICES)
                    {
                        Debug.println("unregisterService: releasing users");
                    }

                    users = new BundleContext[size];
                    contextsUsing.copyInto(users);
                }
            }
        }

        /* must not hold the registrationLock while releasing services */
        for (int i = 0; i < size; i++)
        {
            releaseService(users[i]);
        }

        contextsUsing = null;

        reference = null;       /* mark registration dead */
        context = null;

        /* These fields must be valid after unregister is called:
         *   properties
         */
    }


    /**
     * Returns a {@link ServiceReference} object for this registration.
     * The {@link ServiceReference} object may be shared with other bundles.
     *
     * @exception java.lang.IllegalStateException If
     * this ServiceRegistration has already been unregistered.
     * @return A {@link ServiceReference} object.
     */
    public org.osgi.framework.ServiceReference getReference()
    {
        /* use reference instead of unregistered so that ServiceFactorys, called
         * by releaseService after the registration is unregistered, can
         * get the ServiceReference. Note this technically may voilate the spec
         * but makes more sense.
         */
        if (reference == null)
        {
            throw new IllegalStateException(Msg.formatter.getString("SERVICE_ALREADY_UNREGISTERED_EXCEPTION"));
        }

        return(reference);
    }

    /**
     * Update the properties associated with this service.
     *
     * <p>The key "objectClass" cannot be modified by this method. It's
     * value is set when the service is registered.
     *
     * <p>The following steps are followed to modify a service's properties:
     * <ol>
     * <li>The service's properties are replaced with the provided properties.
     * <li>A {@link ServiceEvent} of type {@link ServiceEvent#MODIFIED}
     * is synchronously sent.
     * </ol>
     *
     * @param properties The properties for this service.
     *        Changes should not be made to this object after calling this method.
     *        To update the service's properties this method should be called again.
     * @exception java.lang.IllegalStateException If
     * this ServiceRegistration has already been unregistered.
     *
     * @exception IllegalArgumentException If the <tt>properties</tt>
     * parameter contains case variants of the same key name.
     */
    public void setProperties(Dictionary props)
    {
        synchronized (registrationLock)
        {
            if (service == null)    /* in the process of unregistering */
            {
                throw new IllegalStateException(Msg.formatter.getString("SERVICE_ALREADY_UNREGISTERED_EXCEPTION"));
            }

            this.properties = createProperties(props);
        }

        /* must not hold the registrationLock when this event is published */
        framework.publishServiceEvent(ServiceEvent.MODIFIED, reference);
    }

    /**
     * Construct a properties object from the dictionary for this
     * ServiceRegistration.
     *
     * @param props The properties for this service.
     * @return A Properties object for this ServiceRegistration.
     */
    protected Properties createProperties(Dictionary props)
    {
        Properties properties = new Properties(props);

        properties.setProperty(Constants.OBJECTCLASS, null); /* remove user provided key if any */
        properties.setProperty(Constants.OBJECTCLASS, clazzes);

        properties.setProperty(Constants.SERVICE_ID, null); /* remove user provided key if any */
        properties.setProperty(Constants.SERVICE_ID, new Long(serviceid));

        Object ranking = properties.getProperty(Constants.SERVICE_RANKING);

        serviceranking = (ranking instanceof Integer) ?
                          ((Integer)ranking).intValue() :
                          0;

        return(properties);
    }

    /**
     * Get the value of a service's property.
     *
     * <p>This method will continue to return property values after the
     * service has been unregistered. This is so that references to
     * unregistered service can be interrogated.
     * (For example: ServiceReference objects stored in the log.)
     *
     * @param key Name of the property.
     * @return Value of the property or <code>null</code> if there is
     * no property by that name.
     */
    protected Object getProperty(String key)
    {
        synchronized (registrationLock)
        {
            return(properties.getProperty(key));
        }
    }

    /**
     * Get the list of key names for the service's properties.
     *
     * <p>This method will continue to return the keys after the
     * service has been unregistered. This is so that references to
     * unregistered service can be interrogated.
     * (For example: ServiceReference objects stored in the log.)
     *
     * @return The list of property key names.
     */
    protected String[] getPropertyKeys()
    {
        synchronized (registrationLock)
        {
            return(properties.getPropertyKeys());
        }
    }

    /**
     * Return the bundle which registered the service.
     *
     * <p>This method will always return <code>null</code> when the
     * service has been unregistered. This can be used to
     * determine if the service has been unregistered.
     *
     * @return The bundle which registered the service.
     * @see BundleContext.registerService
     */
    protected Bundle getBundle()
    {
        if (reference == null)
        {
            return(null);
        }

        return(bundle);
    }

    /**
     * Get a service object for the using BundleContext.
     *
     * @param user BundleContext using service.
     * @return Service object
     */
    protected Object getService(BundleContext user)
    {
        synchronized (registrationLock)
        {
            if (unregistered)   /* service unregistered */
            {
                return(null);
            }

            if (Debug.DEBUG && Debug.DEBUG_SERVICES)
            {
                Debug.println("getService["+user.bundle+"]("+this+")");
            }

            Hashtable servicesInUse = user.servicesInUse;

            ServiceUse use = (ServiceUse)servicesInUse.get(reference);

            if (use == null)
            {
                use = new ServiceUse(user, this);

                Object service = use.getService();

                if (service != null)
                {
                    servicesInUse.put(reference, use);

                    if (contextsUsing == null)
                    {
                        contextsUsing = new Vector(10, 10);
                    }

                    contextsUsing.addElement(user);
                }

                return(service);
            }
            else
            {
                return(use.getService());
            }
        }
    }

    /**
     * Unget a service for the using BundleContext.
     *
     * @param user BundleContext using service.
     * @return <code>false</code> if the context bundle's use count for the service
     *         is zero or if the service has been unregistered,
     *         otherwise <code>true</code>.
     */
    protected boolean ungetService(BundleContext user)
    {
        synchronized (registrationLock)
        {
            if (unregistered)
            {
                return(false);
            }

            if (Debug.DEBUG && Debug.DEBUG_SERVICES)
            {
                    String bundle = (user.bundle == null) ? "" : user.bundle.toString();
                    Debug.println("ungetService["+bundle+"]("+this+")");
            }

            Hashtable servicesInUse = user.servicesInUse;

            if (servicesInUse != null)
            {
                ServiceUse use = (ServiceUse)servicesInUse.get(reference);

                if (use != null)
                {
                    if (use.ungetService())
                    {
                        /* use count is now zero */
                        servicesInUse.remove(reference);

                        contextsUsing.removeElement(user);
                    }

                    return(true);
                }
            }

            return(false);
        }
    }

    /**
     * Release the service for the using BundleContext.
     *
     * @param user BundleContext using service.
     */
    protected void releaseService(BundleContext user)
    {
        synchronized (registrationLock)
        {
            if (reference == null)   /* registration dead */
            {
                return;
            }

            if (Debug.DEBUG && Debug.DEBUG_SERVICES)
            {
                    String bundle = (user.bundle == null) ? "" : user.bundle.toString();
                    Debug.println("releaseService["+bundle+"]("+this+")");
            }

            Hashtable servicesInUse = user.servicesInUse;

            if (servicesInUse != null)
            {
                ServiceUse use = (ServiceUse)servicesInUse.remove(reference);

                if (use != null)
                {
                    use.releaseService();

                    contextsUsing.removeElement(user);
                }
            }
        }
    }

    /**
     * Return the list of bundle which are using this service.
     *
     * @return Array of Bundles using this service.
     */
    protected Bundle[] getUsingBundles()
    {
        synchronized (registrationLock)
        {
            if (unregistered)   /* service unregistered */
            {
                return(null);
            }

            if (contextsUsing == null)
            {
                return(null);
            }

            int size = contextsUsing.size();

            if (size == 0)
            {
                return(null);
            }

            Bundle[] bundles = new Bundle[size];

            /* Copy vector of BundleContext into an array of Bundle. */
            for (int i = 0; i < size; i++)
            {
                bundles[i] = ((BundleContext)contextsUsing.elementAt(i)).bundle;
            }

            return(bundles);
        }
    }

    /**
     * Return a String representation of this object.
     *
     * @return String representation of this object.
     */
    public String toString()
    {
        String[] clazzes = this.clazzes;
        int size = clazzes.length;
        StringBuffer sb = new StringBuffer(50 * size);

        sb.append('{');

        for (int i = 0; i < size; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(clazzes[i]);
        }

        sb.append("}=");
        sb.append(properties);

        return(sb.toString());
    }

    /**
     * Hashtable for service properties.
     */
    static class Properties extends Headers
    {
        /**
         * Create a properties object for the service.
         *
         * @param properties The properties for this service.
         */
        private Properties(int size, Dictionary props)
        {
            super((size << 1) + 1);

            if (props != null)
            {
                synchronized (props)
                {
                    Enumeration enum = props.keys();

                    while (enum.hasMoreElements())
                    {
                        Object key = enum.nextElement();

                        if (key instanceof String)
                        {
                            String header = (String)key;

                            setProperty(header, props.get(header));
                        }
                    }
                }
            }
        }
        /**
         * Create a properties object for the service.
         *
         * @param properties The properties for this service.
         */
        protected Properties(Dictionary props)
        {
            this((props == null) ? 2 : Math.max(2, props.size()), props);
        }

        /**
         * Get a clone of the value of a service's property.
         *
         * @param key header name.
         * @return Clone of the value of the property or <code>null</code> if there is
         * no property by that name.
         */
        protected Object getProperty(String key)
        {
            return(cloneValue(get(key)));
        }

        /**
         * Get the list of key names for the service's properties.
         *
         * @return The list of property key names.
         */
        protected synchronized String[] getPropertyKeys()
        {
            int size = size();

            String[] keynames = new String[size];

            Enumeration enum = keys();

            for (int i = 0; i < size; i++)
            {
                keynames[i] = (String)enum.nextElement();
            }

            return(keynames);
        }

        /**
         * Put a clone of the property value into this property object.
         *
         * @param key Name of property.
         * @param value Value of property.
         * @return previous property value.
         */
        protected synchronized Object setProperty(String key, Object value)
        {
            return(set(key, cloneValue(value)));
        }

        /**
         * Attempt to clone the value if necessary and possible.
         *
         * For some strange reason, you can test to see of an Object is
         * Cloneable but you can't call the clone method since it is
         * protected on Object!
         *
         * @param value object to be cloned.
         * @return cloned object or original object if we didn't clone it.
         */
        protected static Object cloneValue(Object value)
        {
            if (value != null)
            {
                if (value instanceof String)    /* shortcut String */
                {
                    return(value);
                }

                Class clazz = value.getClass();

                if (clazz.isArray())
                {
                    Class type = clazz.getComponentType();

                    if (type.isPrimitive())
                    {
                        if (Integer.TYPE.isAssignableFrom(type))
                        {
                            return(((int[])value).clone());
                        }

                        if (Long.TYPE.isAssignableFrom(type))
                        {
                            return(((long[])value).clone());
                        }

                        if (Byte.TYPE.isAssignableFrom(type))
                        {
                            return(((byte[])value).clone());
                        }

                        if (Short.TYPE.isAssignableFrom(type))
                        {
                            return(((short[])value).clone());
                        }

                        if (Character.TYPE.isAssignableFrom(type))
                        {
                            return(((char[])value).clone());
                        }

                        if (Float.TYPE.isAssignableFrom(type))
                        {
                            return(((float[])value).clone());
                        }

                        if (Double.TYPE.isAssignableFrom(type))
                        {
                            return(((double[])value).clone());
                        }

                        if (Boolean.TYPE.isAssignableFrom(type))
                        {
                            return(((boolean[])value).clone());
                        }
                    }
                    else    /* non-primitive array object */
                    {
                        return(((Object[])value).clone());
                    }
                }
                else    /* non array object */
                {
                    try
                    {
                        return(clazz.getMethod("clone", null).invoke(value, null));
                    }
                    catch (Exception e)
                    {
                        /* clone is not a public method on value's class */
                    }
                    catch (Error e)
                    {
                        /* JCL does not support reflection; try some well known types */

                        if (value instanceof Vector)
                        {
                            return(((Vector)value).clone());
                        }

                        if (value instanceof Hashtable)
                        {
                            return(((Hashtable)value).clone());
                        }
                    }
                }
            }

            return(value);
        }

        public synchronized String toString()
        {
            String keys[] = getPropertyKeys();

            int size = keys.length;

            StringBuffer sb = new StringBuffer(20 * size);

            sb.append('{');

            int n = 0;
            for (int i = 0; i < size; i++)
            {
                String key = keys[i];
                if (!key.equals(Constants.OBJECTCLASS))
                {
                    if (n > 0)
                    {
                        sb.append(", ");
                    }

                    sb.append(key);
                    sb.append('=');
                    sb.append(get(key));
                    n++;
                }
            }

            sb.append('}');

            return(sb.toString());
        }
    }
}

