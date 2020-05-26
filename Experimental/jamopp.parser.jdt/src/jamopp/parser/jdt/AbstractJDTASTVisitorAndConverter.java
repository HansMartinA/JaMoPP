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
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

class AbstractJDTASTVisitorAndConverter extends ASTVisitor {
	private org.emftext.language.java.containers.JavaRoot convertedRootElement;
	private String originalSource;
	
	void setSource(String src) {
		this.originalSource = src;
	}
	
	String getSource() {
		return this.originalSource;
	}
	
	void setConvertedElement(org.emftext.language.java.containers.JavaRoot root) {
		this.convertedRootElement = root;
	}
	
	org.emftext.language.java.containers.JavaRoot getConvertedElement() {
		return this.convertedRootElement;
	}
	
	@Override
	public boolean visit(CompilationUnit node) {
		this.convertedRootElement = org.emftext.language.java.containers.ContainersFactory.eINSTANCE.createEmptyModel();
		return false;
	}
	
	org.emftext.language.java.types.TypeReference convertToClassifierOrNamespaceClassifierReference(Name name) {
		if (name.isSimpleName()) {
			return this.convertToClassifierReference((SimpleName) name);
		} else { // name.isQualifiedName()
			QualifiedName qualifiedName = (QualifiedName) name;
			org.emftext.language.java.types.NamespaceClassifierReference ref = org.emftext.language.java.types.TypesFactory.eINSTANCE.createNamespaceClassifierReference();
			ref.getClassifierReferences().add(this.convertToClassifierReference(qualifiedName.getName()));
			this.convertToNamespacesAndSet(qualifiedName.getQualifier(), ref);
			return ref;
		}
	}
	
	org.emftext.language.java.types.ClassifierReference convertToClassifierReference(SimpleName simpleName) {
		org.emftext.language.java.types.ClassifierReference ref = org.emftext.language.java.types.TypesFactory.eINSTANCE.createClassifierReference();
		org.emftext.language.java.classifiers.Class proxy = org.emftext.language.java.classifiers.ClassifiersFactory.eINSTANCE.createClass();
		((InternalEObject) proxy).eSetProxyURI(null);
		proxy.setName(simpleName.getIdentifier());
		ref.setTarget(proxy);
		return ref;
	}
	
	org.emftext.language.java.imports.Import convertToImport(ImportDeclaration importDecl) {
		if (!importDecl.isOnDemand() && !importDecl.isStatic()) {
			org.emftext.language.java.imports.ClassifierImport convertedImport =
				org.emftext.language.java.imports.ImportsFactory.eINSTANCE.createClassifierImport();
			org.emftext.language.java.classifiers.Class proxy = org.emftext.language.java.classifiers.ClassifiersFactory.eINSTANCE.createClass();
			((InternalEObject) proxy).eSetProxyURI(null);
			convertedImport.setClassifier(proxy);
			this.convertToNamespacesAndSimpleNameAndSet(importDecl.getName(), convertedImport, proxy);
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
			this.convertToNamespacesAndSimpleNameAndSet(qualifiedName.getQualifier(), convertedImport, proxyClass);
			LayoutInformationConverter.convertToMinimalLayoutInformation(convertedImport, importDecl);
			return convertedImport;
		} else if (importDecl.isOnDemand() && !importDecl.isStatic()) {
			org.emftext.language.java.imports.PackageImport convertedImport = org.emftext.language.java.imports.ImportsFactory.eINSTANCE.createPackageImport();
			this.convertToNamespacesAndSet(importDecl.getName(), convertedImport);
			LayoutInformationConverter.convertToMinimalLayoutInformation(convertedImport, importDecl);
			return convertedImport;
		} else { // importDecl.isOnDemand() && importDecl.isStatic()
			org.emftext.language.java.imports.StaticClassifierImport convertedImport = org.emftext.language.java.imports.ImportsFactory.eINSTANCE.createStaticClassifierImport();
			convertedImport.setStatic(org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createStatic());
			org.emftext.language.java.classifiers.Class proxyClass = org.emftext.language.java.classifiers.ClassifiersFactory.eINSTANCE.createClass();
			((InternalEObject) proxyClass).eSetProxyURI(null);
			convertedImport.setClassifier(proxyClass);
			this.convertToNamespacesAndSimpleNameAndSet(importDecl.getName(), convertedImport, proxyClass);
			LayoutInformationConverter.convertToMinimalLayoutInformation(convertedImport, importDecl);
			return convertedImport;
		}
	}
	
	void convertToNamespacesAndSimpleNameAndSet(Name name, org.emftext.language.java.commons.NamespaceAwareElement namespaceElement,
		org.emftext.language.java.commons.NamedElement namedElement) {
		if (name.isSimpleName()) {
			namedElement.setName(((SimpleName) name).getIdentifier());
		} else if (name.isQualifiedName()) {
			QualifiedName qualifiedName = (QualifiedName) name;
			namedElement.setName(qualifiedName.getName().getIdentifier());
			this.convertToNamespacesAndSet(qualifiedName.getQualifier(), namespaceElement);
		}
	}
	
	void convertToNamespacesAndSet(Name name, org.emftext.language.java.commons.NamespaceAwareElement namespaceElement) {
		if (name.isSimpleName()) {
			SimpleName simpleName = (SimpleName) name;
			namespaceElement.getNamespaces().add(0, simpleName.getIdentifier());
		} else if (name.isQualifiedName()) {
			QualifiedName qualifiedName = (QualifiedName) name;
			namespaceElement.getNamespaces().add(0, qualifiedName.getName().getIdentifier());
			convertToNamespacesAndSet(qualifiedName.getQualifier(), namespaceElement);
		}
	}
	
	void convertToSimpleNameOnlyAndSet(Name name, org.emftext.language.java.commons.NamedElement namedElement) {
		if (name.isSimpleName()) {
			SimpleName simpleName = (SimpleName) name;
			namedElement.setName(simpleName.getIdentifier());
		} else { // name.isQualifiedName()
			QualifiedName qualifiedName = (QualifiedName) name;
			namedElement.setName(qualifiedName.getName().getIdentifier());
		}
	}
	
	org.emftext.language.java.modifiers.AnnotationInstanceOrModifier converToModifierOrAnnotationInstance(IExtendedModifier mod) {
		if (mod.isModifier()) {
			return this.convertToModifier((Modifier) mod);
		} else { // mod.isAnnotation()
			return this.convertToAnnotationInstance((Annotation) mod);
		}
	}
	
	org.emftext.language.java.modifiers.Modifier convertToModifier(Modifier mod) {
		org.emftext.language.java.modifiers.Modifier result = null;
		if (mod.isAbstract()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createAbstract();
		} else if (mod.isDefault()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createDefault();
		} else if (mod.isFinal()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createFinal();
		} else if (mod.isNative()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createNative();
		} else if (mod.isPrivate()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createPrivate();
		} else if (mod.isProtected()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createProtected();
		} else if (mod.isPublic()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createPublic();
		} else if (mod.isStatic()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createStatic();
		} else if (mod.isStrictfp()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createStrictfp();
		} else if (mod.isSynchronized()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createSynchronized();
		} else if (mod.isTransient()) {
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createTransient();
		} else { // mod.isVolatile()
			result = org.emftext.language.java.modifiers.ModifiersFactory.eINSTANCE.createVolatile();
		}
		LayoutInformationConverter.convertToMinimalLayoutInformation(result, mod);
		return result;
	}
	
	org.emftext.language.java.annotations.AnnotationInstance convertToAnnotationInstance(Annotation annot) {
		org.emftext.language.java.annotations.AnnotationInstance result = org.emftext.language.java.annotations.AnnotationsFactory.eINSTANCE.createAnnotationInstance();
		org.emftext.language.java.classifiers.Class proxyClass = org.emftext.language.java.classifiers.ClassifiersFactory.eINSTANCE.createClass();
		((InternalEObject) proxyClass).eSetProxyURI(null);
		this.convertToSimpleNameOnlyAndSet(annot.getTypeName(), proxyClass);
		if (annot.isSingleMemberAnnotation()) {
			org.emftext.language.java.annotations.SingleAnnotationParameter param = org.emftext.language.java.annotations.AnnotationsFactory.eINSTANCE
				.createSingleAnnotationParameter();
			result.setParameter(param);
			SingleMemberAnnotation singleAnnot = (SingleMemberAnnotation) annot;
			param.setValue(this.convertToAnnotationValue(singleAnnot.getValue()));
		} else if (annot.isNormalAnnotation()) {
			org.emftext.language.java.annotations.AnnotationParameterList param = org.emftext.language.java.annotations.AnnotationsFactory.eINSTANCE
				.createAnnotationParameterList();
			result.setParameter(param);
			NormalAnnotation normalAnnot = (NormalAnnotation) annot;
			normalAnnot.values().forEach(obj -> {
				MemberValuePair memVal = (MemberValuePair) obj;
				org.emftext.language.java.annotations.AnnotationAttributeSetting attrSet = org.emftext.language.java.annotations.AnnotationsFactory.eINSTANCE
					.createAnnotationAttributeSetting();
				org.emftext.language.java.members.InterfaceMethod methodProxy = org.emftext.language.java.members.MembersFactory.eINSTANCE.createInterfaceMethod();
				((InternalEObject) methodProxy).eSetProxyURI(null);
				this.convertToSimpleNameOnlyAndSet(memVal.getName(), methodProxy);
				attrSet.setAttribute(methodProxy);
				attrSet.setValue(this.convertToAnnotationValue(memVal.getValue()));
				LayoutInformationConverter.convertToMinimalLayoutInformation(attrSet, memVal);
				param.getSettings().add(attrSet);
			});
		}
		LayoutInformationConverter.convertToMinimalLayoutInformation(result, annot);
		return result;
	}
	
	org.emftext.language.java.annotations.AnnotationValue convertToAnnotationValue(Expression expr) {
		if (expr instanceof Annotation) {
			return this.convertToAnnotationInstance((Annotation) expr);
		} else if (expr.getNodeType() == ASTNode.ARRAY_INITIALIZER) {
			return this.convertToArrayInitializer((ArrayInitializer) expr);
		} else if (expr.getNodeType() == ASTNode.CONDITIONAL_EXPRESSION) {
			
		}
		return null;
	}
	
	org.emftext.language.java.arrays.ArrayInitializer convertToArrayInitializer(ArrayInitializer arr) {
		org.emftext.language.java.arrays.ArrayInitializer result = org.emftext.language.java.arrays.ArraysFactory.eINSTANCE.createArrayInitializer();
		arr.expressions().forEach(obj -> {
			org.emftext.language.java.arrays.ArrayInitializationValue value = null;
			Expression expr = (Expression) obj;
			if (expr instanceof ArrayInitializer) {
				value = this.convertToArrayInitializer((ArrayInitializer) expr);
			} else if (expr instanceof Annotation) {
				value = this.convertToAnnotationInstance((Annotation) expr);
			} else {
				value = this.convertToExpression(expr);
			}
			result.getInitialValues().add(value);
		});
		LayoutInformationConverter.convertToMinimalLayoutInformation(result, arr);
		return result;
	}
	
	org.emftext.language.java.expressions.Expression convertToExpression(Expression expr) {
		return null;
	}
}