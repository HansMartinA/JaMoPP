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

package jamopp.parser.jdt;

import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.ModuleModifier;
import org.eclipse.jdt.core.dom.ModulePackageAccess;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.RequiresDirective;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.UsesDirective;

public class JDTASTVisitorAndConverter extends ASTVisitor {
	private org.emftext.language.java.containers.JavaRoot convertedRootElement;
	private String originalSource;
	
	public void setSource(String src) {
		originalSource = src;
	}
	
	public org.emftext.language.java.containers.JavaRoot getConvertedElement() {
		return convertedRootElement;
	}
	
	@Override
	public boolean visit(CompilationUnit node) {
		if (node.getModule() != null) {
			convertedRootElement = convertToModule(node.getModule());
		} else {
			convertedRootElement = org.emftext.language.java.containers.ContainersFactory.eINSTANCE.createEmptyModel();
		}
		node.imports().forEach(obj -> {
			ImportDeclaration importDecl = (ImportDeclaration) obj;
			convertedRootElement.getImports().add(convertToImport(importDecl));
		});
		return false;
	}
	
	private org.emftext.language.java.containers.Module convertToModule(ModuleDeclaration node) {
		org.emftext.language.java.containers.Module module = org.emftext.language.java.containers.ContainersFactory.eINSTANCE.createModule();
		if (node.isOpen()) {
			module.setOpen(org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createOpen());
		}
		LayoutInformationConverter.convertJavaRootLayoutInformation(module, node, originalSource);
		convertToNamespacesAndSet(node.getName(), module);
		node.annotations();
		node.moduleStatements().forEach(statement -> {
			ModuleDirective directive = (ModuleDirective) statement;
			module.getTarget().add(convertToDirective(directive));
		});
		return module;
	}
	
	private org.emftext.language.java.modules.ModuleDirective convertToDirective(ModuleDirective directive) {
		if (directive.getNodeType() == ASTNode.REQUIRES_DIRECTIVE) {
			RequiresDirective reqDir = (RequiresDirective) directive;
			org.emftext.language.java.modules.RequiresModuleDirective result = org.emftext.language.java.modules.ModulesFactory.eINSTANCE.createRequiresModuleDirective();
			reqDir.modifiers().forEach(obj -> {
				ModuleModifier modifier = (ModuleModifier) obj;
				if (modifier.isStatic()) {
					result.setModifier(org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createStatic());
				} else if (modifier.isTransitive()) {
					result.setModifier(org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createTransitive());
				}
			});
			result.setRequiredModule(convertToModuleReference(reqDir.getName()));
			LayoutInformationConverter.convertToMinimalLayoutInformation(result, directive);
			return result;
		} else if (directive.getNodeType() == ASTNode.EXPORTS_DIRECTIVE || directive.getNodeType() == ASTNode.OPENS_DIRECTIVE) {
			ModulePackageAccess accessDir = (ModulePackageAccess) directive;
			org.emftext.language.java.modules.AccessProvidingModuleDirective convertedDir;
			if (directive.getNodeType() == ASTNode.OPENS_DIRECTIVE) {
				convertedDir = org.emftext.language.java.modules.ModulesFactory.eINSTANCE.createOpensModuleDirective();
			} else { // directive.getNodeType() == ASTNode.EXPORTS_DIRECTIVE
				convertedDir = org.emftext.language.java.modules.ModulesFactory.eINSTANCE.createExportsModuleDirective();
			}
			convertToNamespacesAndSet(accessDir.getName(), convertedDir);
			accessDir.modules().forEach(obj -> {
				convertedDir.getModules().add(convertToModuleReference((Name) obj));
			});
			LayoutInformationConverter.convertToMinimalLayoutInformation(convertedDir, directive);
			return convertedDir;
		} else if (directive.getNodeType() == ASTNode.PROVIDES_DIRECTIVE) {
			ProvidesDirective provDir = (ProvidesDirective) directive;
			org.emftext.language.java.modules.ProvidesModuleDirective result = org.emftext.language.java.modules.ModulesFactory.eINSTANCE.createProvidesModuleDirective();
			result.setTypeReference(convertToClassifierOrNamespaceClassifierReference(provDir.getName()));
			provDir.implementations().forEach(obj -> {
				result.getServiceProviders().add(convertToClassifierOrNamespaceClassifierReference((Name) obj));
			});
			LayoutInformationConverter.convertToMinimalLayoutInformation(result, directive);
			return result;
		} else { // directive.getNodeType() == ASTNode.USES_DIRECTIVE
			UsesDirective usDir = (UsesDirective) directive;
			org.emftext.language.java.modules.UsesModuleDirective result = org.emftext.language.java.modules.ModulesFactory.eINSTANCE.createUsesModuleDirective();
			result.setTypeReference(convertToClassifierOrNamespaceClassifierReference(usDir.getName()));
			LayoutInformationConverter.convertToMinimalLayoutInformation(result, directive);
			return result;
		}
	}
	
	private org.emftext.language.java.modules.ModuleReference convertToModuleReference(Name name) {
		org.emftext.language.java.modules.ModuleReference ref = org.emftext.language.java.modules.ModulesFactory.eINSTANCE.createModuleReference();
		org.emftext.language.java.containers.Module modProxy = org.emftext.language.java.containers.ContainersFactory.eINSTANCE.createModule();
		((InternalEObject) modProxy).eSetProxyURI(null);
		ref.setTarget(modProxy);
		convertToNamespacesAndSet(name, modProxy);
		return ref;
	}
	
	private org.emftext.language.java.types.TypeReference convertToClassifierOrNamespaceClassifierReference(Name name) {
		if (name.isSimpleName()) {
			return convertToClassifierReference((SimpleName) name);
		} else { // name.isQualifiedName()
			QualifiedName qualifiedName = (QualifiedName) name;
			org.emftext.language.java.types.NamespaceClassifierReference ref = org.emftext.language.java.types.TypesFactory.eINSTANCE.createNamespaceClassifierReference();
			ref.getClassifierReferences().add(convertToClassifierReference(qualifiedName.getName()));
			convertToNamespacesAndSet(qualifiedName.getQualifier(), ref);
			return ref;
		}
	}
	
	private org.emftext.language.java.types.ClassifierReference convertToClassifierReference(SimpleName simpleName) {
		org.emftext.language.java.types.ClassifierReference ref = org.emftext.language.java.types.TypesFactory.eINSTANCE.createClassifierReference();
		org.emftext.language.java.classifiers.Class proxy = org.emftext.language.java.classifiers.ClassifiersFactory.eINSTANCE.createClass();
		((InternalEObject) proxy).eSetProxyURI(null);
		proxy.setName(simpleName.getIdentifier());
		ref.setTarget(proxy);
		return ref;
	}
	
	private org.emftext.language.java.imports.Import convertToImport(ImportDeclaration importDecl) {
		if (!importDecl.isOnDemand() && !importDecl.isStatic()) {
			org.emftext.language.java.imports.ClassifierImport convertedImport =
				org.emftext.language.java.imports.ImportsFactory.eINSTANCE.createClassifierImport();
			org.emftext.language.java.classifiers.Class proxy = org.emftext.language.java.classifiers.ClassifiersFactory.eINSTANCE.createClass();
			((InternalEObject) proxy).eSetProxyURI(null);
			convertedImport.setClassifier(proxy);
			convertToNamespacesAndSimpleNameAndSet(importDecl.getName(), convertedImport, proxy);
			LayoutInformationConverter.convertToMinimalLayoutInformation(convertedImport, importDecl);
			return convertedImport;
		} else if (!importDecl.isOnDemand() && importDecl.isStatic()) {
			org.emftext.language.java.imports.StaticMemberImport convertedImport =
				org.emftext.language.java.imports.ImportsFactory.eINSTANCE.createStaticMemberImport();
			convertedImport.setStatic(org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createStatic());
			org.emftext.language.java.members.Field proxyMember = org.emftext.language.java.members.MembersFactory.eINSTANCE.createField();
			((InternalEObject) proxyMember).eSetProxyURI(null);
			QualifiedName qualifiedName = (QualifiedName) importDecl.getName();
			proxyMember.setName(qualifiedName.getName().getIdentifier());
			convertedImport.getStaticMembers().add(proxyMember);
			org.emftext.language.java.classifiers.Class proxyClass = org.emftext.language.java.classifiers.ClassifiersFactory.eINSTANCE.createClass();
			((InternalEObject) proxyClass).eSetProxyURI(null);
			convertedImport.setClassifier(proxyClass);
			convertToNamespacesAndSimpleNameAndSet(qualifiedName.getQualifier(), convertedImport, proxyClass);
			LayoutInformationConverter.convertToMinimalLayoutInformation(convertedImport, importDecl);
			return convertedImport;
		} else if (importDecl.isOnDemand() && !importDecl.isStatic()) {
			org.emftext.language.java.imports.PackageImport convertedImport = org.emftext.language.java.imports.ImportsFactory.eINSTANCE.createPackageImport();
			convertToNamespacesAndSet(importDecl.getName(), convertedImport);
			LayoutInformationConverter.convertToMinimalLayoutInformation(convertedImport, importDecl);
			return convertedImport;
		} else { // importDecl.isOnDemand() && importDecl.isStatic()
			org.emftext.language.java.imports.StaticClassifierImport convertedImport = org.emftext.language.java.imports.ImportsFactory.eINSTANCE.createStaticClassifierImport();
			convertedImport.setStatic(org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createStatic());
			org.emftext.language.java.classifiers.Class proxyClass = org.emftext.language.java.classifiers.ClassifiersFactory.eINSTANCE.createClass();
			((InternalEObject) proxyClass).eSetProxyURI(null);
			convertedImport.setClassifier(proxyClass);
			convertToNamespacesAndSimpleNameAndSet(importDecl.getName(), convertedImport, proxyClass);
			LayoutInformationConverter.convertToMinimalLayoutInformation(convertedImport, importDecl);
			return convertedImport;
		}
	}
	
	private void convertToNamespacesAndSimpleNameAndSet(Name name, org.emftext.language.java.commons.NamespaceAwareElement namespaceElement,
		org.emftext.language.java.commons.NamedElement namedElement) {
		if (name.isSimpleName()) {
			namedElement.setName(((SimpleName) name).getIdentifier());
		} else if (name.isQualifiedName()) {
			QualifiedName qualifiedName = (QualifiedName) name;
			namedElement.setName(qualifiedName.getName().getIdentifier());
			convertToNamespacesAndSet(qualifiedName.getQualifier(), namespaceElement);
		}
	}
	
	private void convertToNamespacesAndSet(Name name, org.emftext.language.java.commons.NamespaceAwareElement namespaceElement) {
		if (name.isSimpleName()) {
			SimpleName simpleName = (SimpleName) name;
			namespaceElement.getNamespaces().add(0, simpleName.getIdentifier());
		} else if (name.isQualifiedName()) {
			QualifiedName qualifiedName = (QualifiedName) name;
			namespaceElement.getNamespaces().add(0, qualifiedName.getName().getIdentifier());
			convertToNamespacesAndSet(qualifiedName.getQualifier(), namespaceElement);
		}
	}
}
