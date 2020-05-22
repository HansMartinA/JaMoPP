/*******************************************************************************
 * Copyright (c) 2020, Martin Armbruster
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Armbruster
 *      - Initial implementation
 ******************************************************************************/

package org.emftext.language.java.extensions.references;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.emftext.language.java.references.PackageReference;

public class PackageReferenceExtension {
	private static EList<PackageReference> subpackages;
	
	/**
	 * Returns an empty list that originally contained all sub packages of a PackageReference.
	 * This is a legacy method.
	 * 
	 * @param me the PackageReference.
	 * @return an empty list.
	 * @deprecated All super packages are contained within the namespaces.
	 */
	@Deprecated
	public static EList<PackageReference> getSubpackages(PackageReference me) {
		if (subpackages == null) {
			subpackages = new BasicEList<>();
		}
		return subpackages;
	}
}
