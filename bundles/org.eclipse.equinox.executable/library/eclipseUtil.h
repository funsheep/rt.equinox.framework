/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *******************************************************************************/

#ifndef ECLIPSE_UTIL_H
#define ECLIPSE_UTIL_H

#ifdef UNICODE
#define isJ9VM isJ9VMW
#define isVMLibrary isVMLibraryW
#define checkProvidedVMType checkProvidedVMTypeW
#define checkPath checkPathW
#define checkPathList checkPathListW
#endif

/* constants for checkProvidedVMType */
#define VM_NOTHING		0		/* NULL was given as input */
#define VM_OTHER		1		/* don't know, could be executable or could be nothing */
#define VM_DIRECTORY	2		/* it is a directory */
#define VM_LIBRARY		3		/* it is a library (isVmLibrary would return true) */
#define VM_EE_PROPS		4		/* it is a vm .ee properties file */

/* Eclipse Launcher Utility Methods */

/* Is the given Java VM J9 */
extern int isJ9VM( _TCHAR* vm );

/* Is the given file a shared library? */
extern int isVMLibrary( _TCHAR* vm );

/* determine what the provided -vm argument is referring to */ 
extern int checkProvidedVMType( _TCHAR* vm );

/* check the given path and attempt to make it absolute if it is relative */
extern _TCHAR* checkPath( _TCHAR* path, _TCHAR* programDir, int reverseOrder );

/* take a list of path separated with pathSeparator and run them through checkPath */
extern _TCHAR * checkPathList( _TCHAR* pathList, _TCHAR* programDir, int reverseOrder);

#ifdef AIX 
/* Get the version of the VM */
extern char* getVMVersion( char* vm );

/* Compare JVM Versions */
extern int versionCmp( char* ver1, char* ver2 );
#endif

#endif /* ECLIPSE_UTIL_H */