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

package org.eclipse.osgi.framework.internal.protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;

/**
 * The NullURLStreamService is created when a registered URLStreamHandler service
 * with an associated URLStreamHandlerProxy becomes unregistered.  The associated
 * URLStreamHandlerProxy must still handle all future requests for the now unregistered
 * scheme (the JVM caches URLStreamHandlers making up impossible to "unregister" them). 
 * When requests come in for an unregistered URLStreamHandlerService, the
 * NullURLStreamHandlerService is used in it's place.
 */

public class NullURLStreamHandlerService implements URLStreamHandlerService {

	/**
	 * @see java.net.URLStreamHandler#openConnection(URL)
	 */
	public URLConnection openConnection(URL u) throws IOException 
	{
		throw new MalformedURLException();	
	}

	/**
	 * @see java.net.URLStreamHandler#equals(URL, URL)
	 */
	public boolean equals(URL url1, URL url2) {
		throw new IllegalStateException();	
	}

	/**
	 * @see java.net.URLStreamHandler#getDefaultPort()
	 */
	public int getDefaultPort() {
		throw new IllegalStateException();	
	}

	/**
	 * @see java.net.URLStreamHandler#getHostAddress(URL)
	 */
	public InetAddress getHostAddress(URL url) {
		throw new IllegalStateException();		
	}

	/**
	 * @see java.net.URLStreamHandler#hashCode(URL)
	 */
	public int hashCode(URL url) {
		throw new IllegalStateException();		
	}

	/**
	 * @see java.net.URLStreamHandler#hostsEqual(URL, URL)
	 */
	public boolean hostsEqual(URL url1, URL url2) {
		throw new IllegalStateException();		
	}



	/**
	 * @see java.net.URLStreamHandler#sameFile(URL, URL)
	 */
	public boolean sameFile(URL url1, URL url2) {
		throw new IllegalStateException();		
	}

	/**
	 * @see java.net.URLStreamHandler#setURL(URL, String, String, int, String, String, String, String, String)
	 */
	public void setURL(
		URL u,
		String protocol,
		String host,
		int port,
		String authority,
		String userInfo,
		String file,
		String query,
		String ref) {
		throw new IllegalStateException();		
	}

	/**
	 * @see java.net.URLStreamHandler#setURL(URL, String, String, int, String, String)
	 * @deprecated
	 */
	public void setURL(
		URL u,
		String protocol,
		String host,
		int port,
		String file,
		String ref) {
		throw new IllegalStateException();			
	}

	/**
	 * @see java.net.URLStreamHandler#toExternalForm(URL)
	 */
	public String toExternalForm(URL url) {
		throw new IllegalStateException();		
	}

	/**
	 * @see org.osgi.service.url.URLStreamHandlerService#parseURL(URLStreamHandlerSetter, URL, String, int, int)
	 */
	public void parseURL(
		URLStreamHandlerSetter realHandler,
		URL u,
		String spec,
		int start,
		int limit) {
			throw new IllegalStateException();		
	}

}
