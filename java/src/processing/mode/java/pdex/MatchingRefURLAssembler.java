package processing.mode.java.pdex;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates URLs for MatchingRef errors based on the AST.
 * @author soir20
 */
public class MatchingRefURLAssembler {

    /**
     * Gets the MatchingRef path for a missing array dimension.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getArrDimPath(ASTNode problemNode) {
        ASTNode greatGrandparent = problemNode.getParent().getParent().getParent();
        if (!(greatGrandparent instanceof VariableDeclarationFragment)) {
            return Optional.empty();
        }

        String arrType = problemNode.toString();
        String arrName = ((VariableDeclarationFragment) greatGrandparent).getName().toString();

        return Optional.of("incorrectdimensionexpression1?typename=" + arrType);
    }

    /**
     * Gets the MatchingRef path when the first of two array dimensions is missing.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getTwoDimArrPath(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        ASTNode grandparent = parent.getParent();
        if (!(parent instanceof ArrayCreation) || !(grandparent instanceof VariableDeclarationFragment)) {
            return Optional.empty();
        }

        String arrType = ((ArrayCreation) parent).getType().getElementType().toString();
        String arrName = ((VariableDeclarationFragment) grandparent).getName().toString();

        return Optional.of("incorrectdimensionexpression2?typename=" + arrType);
    }

    /**
     * Gets the MatchingRef path for the use of two array initializers at once.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getTwoInitializerArrPath(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        ASTNode grandparent = parent.getParent();
        if (!(parent instanceof ArrayCreation) || !(grandparent instanceof VariableDeclarationFragment)) {
            return Optional.empty();
        }

        String arrType = ((ArrayCreation) parent).getType().getElementType().toString();
        String arrName = ((VariableDeclarationFragment) grandparent).getName().toString();

        return Optional.of("incorrectdimensionexpression3?typename=" + arrType);
    }

    /**
     * Gets the MatchingRef path for a missing method.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getMissingMethodPath(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        if (!(parent instanceof MethodInvocation)) {
            return Optional.empty();
        }

        MethodInvocation invocation = (MethodInvocation) problemNode.getParent();
        List<String> providedParams = ((List<?>) invocation.arguments()).stream()
                .map(Object::toString).collect(Collectors.toList());
        List<String> providedParamTypes = ((List<?>) invocation.arguments()).stream().map(
                (param) -> ((Expression) param).resolveTypeBinding().getName()
        ).collect(Collectors.toList());
        String methodName = invocation.getName().toString();

        /* We don't know the desired return type, so use a
           familiar one like "int" instead of one like "void." */
        String dummyReturnType = "int";
        String dummyCorrectName = "correctName";

        return Optional.of("methodnotfound?methodname=" + methodName
                + "&correctmethodname=" + dummyCorrectName
                + "&typename=" + dummyReturnType);
    }

    /**
     * Gets the MatchingRef path for a parameter mismatch in a method call.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getParamMismatchPath(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        if (!(parent instanceof MethodInvocation)) {
            return Optional.empty();
        }

        MethodInvocation invocation = (MethodInvocation) parent;
        List<String> providedParams = ((List<?>) invocation.arguments()).stream()
                .map(Object::toString).collect(Collectors.toList());
        List<String> providedParamTypes = ((List<?>) invocation.arguments()).stream().map(
                (param) -> ((Expression) param).resolveTypeBinding().getName()
        ).collect(Collectors.toList());
        List<String> requiredParamTypes = Arrays.stream(
                invocation.resolveMethodBinding().getParameterTypes()
        ).map(ITypeBinding::getName).collect(Collectors.toList());

        String methodName = invocation.getName().toString();
        String methodReturnType = invocation.resolveMethodBinding().getReturnType().toString();

        return Optional.of("parametermismatch?methodname=" + methodName
                + "&methodtypename=" + methodReturnType
                + "&typeonename=int&typetwoname=String");
    }

    /**
     * Gets the MatchingRef path for a missing return statement in a method.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getMissingReturnPath(ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        if (!(parent instanceof MethodDeclaration)) {
            return Optional.empty();
        }

        MethodDeclaration declaration = (MethodDeclaration) parent;
        List<String> requiredParamTypes = Arrays.stream(
                declaration.resolveBinding().getParameterTypes()
        ).map(ITypeBinding::getName).collect(Collectors.toList());
        String methodName = declaration.getName().toString();
        String methodReturnType = declaration.getReturnType2().toString();

        return Optional.of("returnmissing?methodname=" + methodName
                + "&typename=" + methodReturnType);
    }

    /**
     * Gets the MatchingRef path for a mismatch between a variable's type and its assigned value.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getTypeMismatchPath(String providedType, String requiredType, ASTNode problemNode) {
        ASTNode parent = problemNode.getParent();
        if (!(parent instanceof VariableDeclarationFragment)) {
            return Optional.empty();
        }

        String varName = ((VariableDeclarationFragment) parent).getName().toString();

        return Optional.of("typemismatch?typeonename=" + providedType
                + "&typetwoname=" + requiredType
                + "&varname=" + varName);
    }

    /**
     * Gets the MatchingRef path for a missing type.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getMissingTypePath(String missingType, ASTNode problemNode) {
        ASTNode grandparent = problemNode.getParent().getParent();

        // All variables in the statement will be the same type, so use the first as an example
        VariableDeclarationFragment firstVar;
        if (grandparent instanceof VariableDeclarationStatement) {
            VariableDeclarationStatement varStatement = (VariableDeclarationStatement) grandparent;
            firstVar = (VariableDeclarationFragment) varStatement.fragments().get(0);
        } else if (grandparent.getParent() instanceof VariableDeclarationFragment) {
            firstVar = (VariableDeclarationFragment) grandparent.getParent();
        } else {
            return Optional.empty();
        }

        String varName = firstVar.getName().toString();
        String dummyCorrectName = "CorrectName";

        return Optional.of("typenotfound?classname=" + missingType
                + "&correctclassname=" + dummyCorrectName
                + "&varname=" + varName);
    }

    /**
     * Gets the MatchingRef path for a missing variable.
     * @param problemNode       node of the AST where the problem occurred
     * @return the path and parameters for the corresponding MatchingRef page
     */
    public Optional<String> getMissingVarPath(String varName, ASTNode problemNode) {
        String params = "?";
        ASTNode parent = problemNode.getParent();
        ASTNode grandparent = parent.getParent();

        System.out.println(varName);
        System.out.println(problemNode.getClass());
        System.out.println(parent.getClass());
        System.out.println(grandparent.getClass());

        if (parent instanceof MethodInvocation) {
            MethodInvocation invocation = (MethodInvocation) parent;
            List<String> requiredParamTypes = Arrays.stream(
                    invocation.resolveMethodBinding().getParameterTypes()
            ).map(ITypeBinding::getName).collect(Collectors.toList());
            List<String> providedParams = ((List<?>) invocation.arguments()).stream()
                    .map(Object::toString).collect(Collectors.toList());

            String varType = requiredParamTypes.get(providedParams.indexOf(varName));
            params += "classname=" + varType + "&varname=" + varName;

        } else if (parent instanceof ArrayAccess && grandparent instanceof VariableDeclarationFragment) {
            ArrayAccess arrAccess = (ArrayAccess) parent;
            String length = arrAccess.getIndex().toString();

            // The "varName" is actually the declared type when an array is being created
            VariableDeclarationFragment declaration = (VariableDeclarationFragment) grandparent;
            String arrName = declaration.getName().toString();

            params += "classname=" + varName + "&varname=" + arrName;
        } else {
            return Optional.empty();
        }

        return Optional.of("variablenotfound" + params);
    }



}
