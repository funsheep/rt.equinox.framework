/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.dependencies;

/**
 * Not to be implemented by clients.
 */
public interface IElement {
	public Object getId();
	public Object getVersionId();
	/** @return a non-null reference */
	public IDependency[] getDependencies();
	/** may return null */
	public IDependency getDependency(Object id);
	public boolean isSingleton();
	public Object getUserObject();
}