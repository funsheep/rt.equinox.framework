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
package org.eclipse.osgi.internal.resolver;

import java.util.Dictionary;

import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.osgi.framework.BundleException;

public class StateObjectFactoryImpl implements StateObjectFactory {
	public BundleDescription createBundleDescription(Dictionary manifest, String location, long id) {
		BundleDescriptionImpl result;
		try {
			result = (BundleDescriptionImpl) StateBuilder.createBundleDescription(manifest, location);
		} catch (BundleException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		result.setBundleId(id);
		return result;
	}
	public BundleDescription createBundleDescription(long id, String globalName, Version version, String location, BundleSpecification[] required, HostSpecification host,PackageSpecification[] packages, String[] providedPackages) {
		BundleDescriptionImpl bundle = new BundleDescriptionImpl();
		bundle.setBundleId(id);
		bundle.setUniqueId(globalName);
		bundle.setVersion(version);
		bundle.setLocation(location);
		bundle.setRequiredBundles(required);
		bundle.setPackages(packages);
		bundle.setHost(host);
		//TODO: need to fix the state API to use String[] to represent provided packages 
		//bundle.setProvidedPackages(providedPackages);
		return bundle;		
	}
	public BundleDescription createBundleDescription(BundleDescription original) {
		BundleDescriptionImpl bundle = new BundleDescriptionImpl();
		bundle.setBundleId(original.getBundleId());
		bundle.setUniqueId(original.getUniqueId());
		bundle.setVersion(original.getVersion());
		bundle.setLocation(original.getLocation());
		BundleSpecification[] originalRequired = original.getRequiredBundles();
		BundleSpecification[] newRequired = new BundleSpecification[originalRequired.length];
		for (int i = 0; i < newRequired.length; i++)
			newRequired[i] = createBundleSpecification(originalRequired[i]);		
		bundle.setRequiredBundles(newRequired);
		PackageSpecification[] originalPackages = original.getPackages();
		PackageSpecification[] newPackages = new PackageSpecification[originalPackages.length];
		for (int i = 0; i < newPackages.length; i++)
			newPackages[i] = createPackageSpecification(originalPackages[i]);		
		bundle.setPackages(newPackages);
		if (original.getHost() != null) 
			bundle.setHost(createHostSpecification(original.getHost()));
		String[] originalProvidedPackages = original.getProvidedPackages();
		String[] newProvidedPackages = new String[originalProvidedPackages.length];
		System.arraycopy(originalProvidedPackages,0,newProvidedPackages,0,originalProvidedPackages.length);
		bundle.setProvidedPackages(newProvidedPackages);		
		return bundle;			
	}
	public BundleSpecification createBundleSpecification(BundleDescription bundle, String requiredGlobalName, Version requiredVersion, byte matchingRule, boolean export, boolean optional) {
		BundleSpecificationImpl bundleSpec = new BundleSpecificationImpl();		
		bundleSpec.setBundle(bundle);
		bundleSpec.setName(requiredGlobalName);
		bundleSpec.setVersionSpecification(requiredVersion);
		bundleSpec.setMatchingRule(matchingRule);
		bundleSpec.setExported(export);
		bundleSpec.setOptional(optional);
		return bundleSpec;		
	}
	public BundleSpecification createBundleSpecification(BundleSpecification original) {
		BundleSpecificationImpl bundleSpec = new BundleSpecificationImpl();		
		bundleSpec.setName(original.getName());
		bundleSpec.setVersionSpecification(original.getVersionSpecification());
		bundleSpec.setMatchingRule(original.getMatchingRule());
		bundleSpec.setExported(original.isExported());
		bundleSpec.setOptional(original.isOptional());
		return bundleSpec;		
	}
	public HostSpecification createHostSpecification(BundleDescription bundle, String hostGlobalName, Version hostVersion, byte matchingRule, boolean reloadHost) {
		HostSpecificationImpl hostSpec = new HostSpecificationImpl();		
		hostSpec.setBundle(bundle);
		hostSpec.setName(hostGlobalName);
		hostSpec.setVersionSpecification(hostVersion);
		hostSpec.setMatchingRule(matchingRule);
		hostSpec.setReloadHost(reloadHost);
		return hostSpec;
	}
	public HostSpecification createHostSpecification(HostSpecification original) {
		HostSpecificationImpl hostSpec = new HostSpecificationImpl();		
		hostSpec.setName(original.getName());
		hostSpec.setVersionSpecification(original.getVersionSpecification());
		hostSpec.setMatchingRule(original.getMatchingRule());
		hostSpec.setReloadHost(original.reloadHost());
		return hostSpec;
	}
	public PackageSpecification createPackageSpecification(BundleDescription bundle, String packageName, Version packageVersion, boolean exported) {
		PackageSpecificationImpl packageSpec = new PackageSpecificationImpl();		
		packageSpec.setBundle(bundle);
		packageSpec.setName(packageName);
		packageSpec.setVersionSpecification(packageVersion);
		packageSpec.setExport(exported);
		return packageSpec;
	}	
	public PackageSpecification createPackageSpecification(PackageSpecification original) {
		PackageSpecificationImpl packageSpec = new PackageSpecificationImpl();		
		packageSpec.setName(original.getName());
		packageSpec.setVersionSpecification(original.getVersionSpecification());
		packageSpec.setExport(original.isExported());
		return packageSpec;
	}
	public State createState() {
		return new StateImpl();
	}
	public State createState(State original) {
		StateImpl newState = new StateImpl();
		BundleDescription[] bundles = original.getBundles();
		for (int i = 0; i < bundles.length; i++)
			newState.addBundle(createBundleDescription(bundles[i]));
		return newState;
	}
}
