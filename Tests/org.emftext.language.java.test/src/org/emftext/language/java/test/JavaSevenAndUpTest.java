/*******************************************************************************
 * Copyright (c) 2019-2020, Martin Armbruster
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

package org.emftext.language.java.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.emftext.language.java.classifiers.Interface;
import org.emftext.language.java.containers.CompilationUnit;
import org.emftext.language.java.containers.JavaRoot;
import org.emftext.language.java.expressions.ArrayConstructorReferenceExpression;
import org.emftext.language.java.expressions.AssignmentExpression;
import org.emftext.language.java.expressions.ClassTypeConstructorReferenceExpression;
import org.emftext.language.java.expressions.LambdaExpression;
import org.emftext.language.java.expressions.PrimaryExpressionReferenceExpression;
import org.emftext.language.java.instantiations.NewConstructorCall;
import org.emftext.language.java.literals.BinaryIntegerLiteral;
import org.emftext.language.java.literals.BinaryLongLiteral;
import org.emftext.language.java.members.Constructor;
import org.emftext.language.java.members.Member;
import org.emftext.language.java.members.Method;
import org.emftext.language.java.modifiers.Default;
import org.emftext.language.java.modifiers.Modifier;
import org.emftext.language.java.modifiers.Static;
import org.emftext.language.java.modules.NormalModule;
import org.emftext.language.java.parameters.CatchParameter;
import org.emftext.language.java.parameters.ReceiverParameter;
import org.emftext.language.java.statements.Block;
import org.emftext.language.java.statements.EmptyStatement;
import org.emftext.language.java.statements.ExpressionStatement;
import org.emftext.language.java.statements.LocalVariableStatement;
import org.emftext.language.java.statements.Statement;
import org.emftext.language.java.statements.TryBlock;
import org.emftext.language.java.types.InferableType;
import org.junit.Test;

/**
 * Test class for the features of Java 7+.
 * 
 * @author Martin Armbruster
 */
public class JavaSevenAndUpTest extends AbstractJavaParserTestCase {
	@Override
	public String getTestInputFolder() {
		return ".." + File.separator + "org.emftext.language.java.tests.sevenup" + File.separator + "src";
	}
	
	@Override
	public boolean isExcludedFromReprintTest(String file) {
		return false;
	}
	
	@Test
	public void testModuleInfo() {
		try {
			String file = "module-info.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, NormalModule.class);
			assertEquals(6, ((NormalModule) root).getTarget().size());
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testPackageInfo() {
		try {
			String file = "simplepackage" + File.separator + "package-info.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, org.emftext.language.java.containers.Package.class);
			assertEquals("simplepackage", ((org.emftext.language.java.containers.Package) root).getName());
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleClassWithLambdaExpressions() {
		try {
			String file = "pkg2" + File.separator + "SimpleClassWithLambdaExpressions.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			CompilationUnit unit = (CompilationUnit) root;
			this.assertNumberOfClassifiers(unit, 1);
			org.emftext.language.java.classifiers.Class classifier = unit.getContainedClass();
			this.assertClassifierName(classifier, "SimpleClassWithLambdaExpressions");
			this.assertMemberCount(classifier, 2);
			for (Member m : classifier.getMembers()) {
				if (m instanceof Interface) {
					assertEquals("I", m.getName());
					assertEquals(1, ((Interface) m).getAnnotationInstances().size());
				} else if (m instanceof Method) {
					assertEquals("lambdas", m.getName());
					this.assertIsPublic((Method) m);
					this.assertType(((Method) m).getTypeReference(), org.emftext.language.java.types.Void.class);
					for (Statement s : ((Method) m).getBlock().getStatements()) {
						assertTrue(s instanceof ExpressionStatement || s instanceof LocalVariableStatement);
						if (s instanceof ExpressionStatement) {
							ExpressionStatement castedS = (ExpressionStatement) s;
							this.assertType(castedS.getExpression(), AssignmentExpression.class);
							AssignmentExpression expr = (AssignmentExpression) castedS.getExpression();
							this.assertType(expr.getValue(), LambdaExpression.class);
						} else if (s instanceof LocalVariableStatement) {
							LocalVariableStatement castedS = (LocalVariableStatement) s;
							this.assertType(castedS.getVariable().getInitialValue(), LambdaExpression.class);
						}
					}
				}
			}
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleClassWithLiterals() {
		try {
			String file = "pkg2" + File.separator + "SimpleClassWithLiterals.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			CompilationUnit unit = (CompilationUnit) root;
			this.assertNumberOfClassifiers(unit, 1);
			org.emftext.language.java.classifiers.Class classifier = unit.getContainedClass();
			assertEquals(1, classifier.getMembers().size());
			Method m = (Method) classifier.getMembers().get(0);
			for (Statement s : m.getBlock().getStatements()) {
				assertTrue(s instanceof LocalVariableStatement || s instanceof ExpressionStatement);
				if (s instanceof LocalVariableStatement) {
					LocalVariableStatement castedS = (LocalVariableStatement) s;
					assertTrue(castedS.getVariable().getInitialValue() instanceof BinaryIntegerLiteral
							|| castedS.getVariable().getInitialValue() instanceof BinaryLongLiteral);
				} else if (s instanceof ExpressionStatement) {
					AssignmentExpression expr = ((AssignmentExpression) ((ExpressionStatement) s).getExpression());
					assertTrue(expr.getValue() instanceof BinaryIntegerLiteral
							|| expr.getValue() instanceof BinaryLongLiteral);
				}
			}
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleClassWithTryCatch() {
		try {
			String file = "pkg2" + File.separator + "SimpleClassWithTryCatch.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			CompilationUnit unit = (CompilationUnit) root;
			this.assertNumberOfClassifiers(unit, 1);
			org.emftext.language.java.classifiers.Class classifier = unit.getContainedClass();
			this.assertMemberCount(classifier, 3);
			Method method = (Method) classifier.getMembers().get(1);
			this.assertIsPublic(method);
			assertEquals("tryCatch", method.getName());
			assertEquals(6, method.getBlock().getStatements().size());
			TryBlock tryBlock = (TryBlock) method.getBlock().getStatements().get(0);
			assertEquals(1, tryBlock.getBlock().getStatements().size());
			assertEquals(1, tryBlock.getResources().size());
			assertEquals(1, tryBlock.getCatchBlocks().size());
			assertTrue(tryBlock.getFinallyBlock() != null);
			tryBlock = (TryBlock) method.getBlock().getStatements().get(1);
			assertEquals(1, tryBlock.getBlock().getStatements().size());
			assertEquals(1, tryBlock.getResources().size());
			assertEquals(1, tryBlock.getCatchBlocks().size());
			assertTrue(tryBlock.getFinallyBlock() == null);
			tryBlock = (TryBlock) method.getBlock().getStatements().get(2);
			assertEquals(1, tryBlock.getBlock().getStatements().size());
			assertEquals(2, tryBlock.getResources().size());
			assertEquals(1, tryBlock.getCatchBlocks().size());
			assertTrue(tryBlock.getFinallyBlock() == null);
			tryBlock = (TryBlock) method.getBlock().getStatements().get(3);
			assertEquals(1, tryBlock.getBlock().getStatements().size());
			assertEquals(0, tryBlock.getResources().size());
			assertEquals(1, tryBlock.getCatchBlocks().size());
			this.assertType(tryBlock.getCatchBlocks().get(0).getParameter(), CatchParameter.class);
			CatchParameter catchParam = (CatchParameter) tryBlock.getCatchBlocks().get(0).getParameter();
			assertEquals(3, catchParam.getTypeReferences().size());
			assertTrue(tryBlock.getFinallyBlock() == null);
			tryBlock = (TryBlock) method.getBlock().getStatements().get(4);
			assertEquals(0, tryBlock.getBlock().getStatements().size());
			assertEquals(1, tryBlock.getResources().size());
			assertEquals(0, tryBlock.getCatchBlocks().size());
			assertTrue(tryBlock.getFinallyBlock() == null);
			tryBlock = (TryBlock) method.getBlock().getStatements().get(5);
			assertEquals(0, tryBlock.getBlock().getStatements().size());
			assertEquals(1, tryBlock.getResources().size());
			assertEquals(0, tryBlock.getCatchBlocks().size());
			assertTrue(tryBlock.getFinallyBlock() != null);
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleInterfaceWithDefaultMethods() {
		try {
			String file = "pkg2" + File.separator + "SimpleInterfaceWithDefaultMethods.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			CompilationUnit unit = (CompilationUnit) root;
			this.assertNumberOfClassifiers(unit, 1);
			Interface classifier = unit.getContainedInterface();
			this.assertMemberCount(classifier, 5);
			int numberOfDefaultOrStaticMethods = 0;
			for (Member member : classifier.getMembers()) {
				if (member instanceof Method) {
					Method method = (Method) member;
					boolean hasStaticOrDefaultModifier = false;
					for (Modifier modifier : method.getModifiers()) {
						if (modifier instanceof Static || modifier instanceof Default) {
							hasStaticOrDefaultModifier = true;
							numberOfDefaultOrStaticMethods++;
						}
					}
					if (hasStaticOrDefaultModifier) {
						this.assertType(method.getStatement(), Block.class);
					} else {
						this.assertType(method.getStatement(), EmptyStatement.class);
					}
				}
			}
			assertEquals(3, numberOfDefaultOrStaticMethods);
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleClassWithDiamondTypeArguments() {
		try {
			String file = "simplepackage" + File.separator + "SimpleClassWithDiamondTypeArguments.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			CompilationUnit unit = (CompilationUnit) root;
			this.assertNumberOfClassifiers(unit, 1);
			org.emftext.language.java.classifiers.Class classifier = unit.getContainedClass();
			this.assertMemberCount(classifier, 2);
			Method method = (Method) classifier.getMembers().get(0);
			assertEquals(6, method.getBlock().getStatements().size());
			for (Statement s : method.getBlock().getStatements()) {
				assertTrue(s instanceof LocalVariableStatement || s instanceof ExpressionStatement);
				if (s instanceof LocalVariableStatement) {
					LocalVariableStatement castedS = (LocalVariableStatement) s;
					this.assertType(castedS.getVariable().getInitialValue(), NewConstructorCall.class);
				} else {
					this.assertType(((ExpressionStatement) s).getExpression(), NewConstructorCall.class);
				}
			}
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleClassWithMethodReferenceExpressions() {
		try {
			String file = "simplepackage" + File.separator + "SimpleClassWithMethodReferenceExpressions.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			CompilationUnit unit = (CompilationUnit) root;
			this.assertNumberOfClassifiers(unit, 1);
			org.emftext.language.java.classifiers.Class classifier = unit.getContainedClass();
			this.assertMemberCount(classifier, 2);
			Method method = (Method) classifier.getMembers().get(0);
			assertEquals(8, method.getBlock().getStatements().size());
			LocalVariableStatement locStat = (LocalVariableStatement) method.getBlock().getStatements().get(0);
			this.assertType(locStat.getVariable().getInitialValue(), PrimaryExpressionReferenceExpression.class);
			locStat = (LocalVariableStatement) method.getBlock().getStatements().get(1);
			this.assertType(locStat.getVariable().getInitialValue(), ClassTypeConstructorReferenceExpression.class);
			AssignmentExpression expr = (AssignmentExpression) ((ExpressionStatement) method.getBlock().getStatements().get(2)).getExpression();
			this.assertType(expr.getValue(), ClassTypeConstructorReferenceExpression.class);
			expr = (AssignmentExpression) ((ExpressionStatement) method.getBlock().getStatements().get(3)).getExpression();
			this.assertType(expr.getValue(), ClassTypeConstructorReferenceExpression.class);
			expr = (AssignmentExpression) ((ExpressionStatement) method.getBlock().getStatements().get(4)).getExpression();
			this.assertType(expr.getValue(), PrimaryExpressionReferenceExpression.class);
			locStat = (LocalVariableStatement) method.getBlock().getStatements().get(5);
			this.assertType(locStat.getVariable().getInitialValue(), PrimaryExpressionReferenceExpression.class);
			locStat = (LocalVariableStatement) method.getBlock().getStatements().get(6);
			this.assertType(locStat.getVariable().getInitialValue(), ArrayConstructorReferenceExpression.class);
			expr = (AssignmentExpression) ((ExpressionStatement) method.getBlock().getStatements().get(7)).getExpression();
			this.assertType(expr.getValue(), ArrayConstructorReferenceExpression.class);
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleClassWithReceiverParameters() {
		try {
			String file = "simplepackage" + File.separator + "SimpleClassWithReceiverParameters.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			CompilationUnit unit = (CompilationUnit) root;
			this.assertNumberOfClassifiers(unit, 1);
			org.emftext.language.java.classifiers.Class classifier = unit.getContainedClass();
			this.assertMemberCount(classifier, 7);
			for (Member member : classifier.getMembers()) {
				if (member instanceof Constructor) {
					assertTrue(0 == ((Constructor) member).getParameters().size()
							|| 1 == ((Constructor) member).getParameters().size());
				} else if (member instanceof Method) {
					Method method = (Method) member;
					this.assertType(method.getParameters().get(0), ReceiverParameter.class);
					for (int i = 1; i < method.getParameters().size(); i++) {
						assertFalse(method.getParameters().get(i) instanceof ReceiverParameter);
					}
				} else if (member instanceof org.emftext.language.java.classifiers.Class) {
					org.emftext.language.java.classifiers.Class innerClass = (org.emftext.language.java.classifiers.Class) member;
					this.assertMemberCount(innerClass, 3);
					for (Member innerMember : innerClass.getMembers()) {
						if (innerMember instanceof Constructor) {
							this.assertType(((Constructor) innerMember).getParameters().get(0), ReceiverParameter.class);
						} else if (innerMember instanceof Method) {
							this.assertType(((Method) innerMember).getParameters().get(0), ReceiverParameter.class);
						} else {
							fail("There should be no other member.");
						}
					}
				} else {
					fail("There should be no other member.");
				}
			}
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleClassWithVar() {
		try {
			String file = "simplepackage" + File.separator + "SimpleClassWithVar.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			CompilationUnit unit = (CompilationUnit) root;
			this.assertNumberOfClassifiers(unit, 1);
			org.emftext.language.java.classifiers.Class classifier = unit.getContainedClass();
			this.assertMemberCount(classifier, 6);
			for (Member member : classifier.getMembers()) {
				if (member instanceof Method) {
					Method method = (Method) member;
					assertEquals(8, method.getBlock().getStatements().size());
					LocalVariableStatement locStat = (LocalVariableStatement) method.getBlock().getStatements().get(0);
					this.assertType(locStat.getVariable().getTypeReference().getTarget(), InferableType.class);
				}
			}
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleClassWithRestrictedKeywords() {
		try {
			String file = "simplepackage" + File.separator + "SimpleClassWithRestrictedKeywords.java";
			JavaRoot root = this.parseResource(file);
			this.assertType(root, CompilationUnit.class);
			this.assertResolveAllProxies(root);
			this.parseAndReprint(file);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
