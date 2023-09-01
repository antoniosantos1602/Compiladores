package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.HashMap;
import java.util.List;

public class JmmSemanticMethodCheck extends AJmmVisitor<List<Report>, Type> {

    String methodName;
    JmmSymbolTable symbolTable;
    HashMap<String, Boolean> localVariablesInitialized;


    public JmmSemanticMethodCheck(JmmSymbolTable symbolTable, String methodName) {
        this.symbolTable = symbolTable;
        this.methodName = methodName;
        this.localVariablesInitialized = new HashMap<>();

        JmmSymbolTableMethod method = symbolTable.getMethodsMap().get(methodName);

        for (Symbol symbol : method.getLocalVariables()) {
            localVariablesInitialized.put(symbol.getName(), false);
        }

    }

    @Override
    protected void buildVisitor() {
        addVisit("Method", this::visitMethod);
        addVisit("LocalVar", this::visitLocalVar);
        addVisit("While", this::visitWhile);
        addVisit("IfElse", this::visitIfElse);
        addVisit("ArrayAssignment", this::visitArrayAssignment);
        addVisit("Assignment", this::visitAssignment);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("ArrayLength", this::visitArrayLength);
        addVisit("NewIntArray", this::visitNewIntArray);
        addVisit("NewObject", this::visitNewObject);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("Integer", this::visitInteger);
        addVisit("True", this::visitTrue);
        addVisit("False", this::visitFalse);
        addVisit("This", this::visitThis);
        setDefaultVisit(this::defaultVisit);
    }

    private Type visitMethod(JmmNode node, List<Report> reports) {

        for (int i = 0; i < node.getNumChildren()-1; i++) {
            visit(node.getJmmChild(i), reports);
        }

        Type returnType = visit(node.getJmmChild(node.getNumChildren()-1), reports);
        if (returnType != null) {
            node.put("expectedType", returnType.getName());
        }

        String methodName = node.get("methodName");
        Type methodType = symbolTable.getMethodsMap().get(methodName).getReturnType();

        if (returnType != null && returnType.equals(new Type("import", false))) {
            returnType = methodType;
        }

        if (!methodType.equals(returnType)) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Method " + methodName + " must return a " + methodType + ". Instead, it returns a " + returnType + "."));
            return null;
        }

        return methodType;
    }

    private Type visitLocalVar(JmmNode node, List<Report> reports) {
        String varName = node.get("name");
        if (localVariablesInitialized.containsKey(varName)) {
            localVariablesInitialized.put(varName, true);
        }
        return symbolTable.getMethodsMap().get(methodName).getLocalVariable(varName).getType();
    }

    private Type visitWhile(JmmNode node, List<Report> reports) {
        Type typeCondition = visit(node.getJmmChild(0), reports);

        if (typeCondition == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "While condition cannot be used."));
            return null;
        }

        if (!typeCondition.equals(new Type("boolean", false)) && !typeCondition.equals(new Type("import", false))) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Condition must be a boolean."));
        }

        for (int i = 1; i < node.getNumChildren(); i++) {
            visit(node.getJmmChild(i), reports);
        }

        return null;
    }

    private Type visitIfElse(JmmNode node, List<Report> reports) {
        Type typeCondition = visit(node.getJmmChild(0), reports);

        if (typeCondition == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "If condition cannot be used."));
            return null;
        }

        if (!typeCondition.equals(new Type("boolean", false)) && !typeCondition.equals(new Type("import", false))) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Condition must be a boolean."));
        }

        for (int i = 1; i < node.getNumChildren(); i++) {
            visit(node.getJmmChild(i), reports);
        }

        return null;
    }

    private Type visitArrayAssignment(JmmNode node, List<Report> reports) {
        Type typeArray = getTypeOfID(node.get("var"), reports);
        Type typeIndex = visit(node.getJmmChild(0), reports);
        Type typeExpression = visit(node.getJmmChild(1), reports);
        if(typeArray != null) {
            node.put("expectedType", typeArray.getName());
        }

        if (typeArray == null || typeIndex == null || typeExpression == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Something went wrong with array assignment."));
            return null;
        }

        if (!typeArray.equals(new Type("int", true)) && !typeArray.equals(new Type("import", false))) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Cannot assign to a non-array variable."));
            return null;
        }

        if (!typeIndex.equals(new Type("int", false)) && !typeIndex.equals(new Type("import", false))) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Array index must be an integer."));
            return null;
        }

        if (!typeExpression.equals(new Type("int", false)) && !typeExpression.equals(new Type("import", false))) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Cannot assign " + typeExpression + " to an array of integers."));
            return null;
        }

        return typeExpression;
    }

    private Type visitAssignment(JmmNode node, List<Report> reports) {
        Type typeAssignment = getTypeOfID(node.get("var"), reports);
        Type typeExpression = visit(node.getJmmChild(0), reports);
        if(typeAssignment != null) {
            node.put("expectedType", typeAssignment.getName());
        }

        if (typeAssignment == null || typeExpression == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Something went wrong with assignment. Tried to assign " + typeExpression + " to " + typeAssignment));
            return null;
        }

        if (typeAssignment.equals(typeExpression) || typeExpression.equals(new Type("import", false))) {
            return typeAssignment;
        }

        if (typeAssignment.equals(new Type(symbolTable.getSuper(), false)) && typeExpression.equals(new Type(symbolTable.getClassName(), false))) {
            return typeAssignment;
        }

        if (symbolTable.getImportsLastName().contains(typeExpression.getName()) && symbolTable.getImportsLastName().contains(typeAssignment.getName())) {
            return typeAssignment;
        }

        reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Cannot assign " + typeExpression + " to " + typeAssignment));
        return null;
    }


    private Type visitMethodCall(JmmNode node, List<Report> reports) {
        Type type = visit(node.getJmmChild(0), reports);
        String methodName = node.get("methodName");
        symbolTable.setCurrentMethod(methodName);
        JmmSymbolTableMethod method = symbolTable.getMethodsMap().get(methodName);

        if (method == null) {

            if (type == null) {
                reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Method " + methodName + " not found."));
                return null;
            }

            if (symbolTable.getImportsLastName().contains(type.getName())) {
                return new Type("import", false);
            }

            if (type.equals(new Type(symbolTable.getClassName(), false)) && !symbolTable.getSuper().equals("")) {
                return new Type("import", false);
            }

            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Method " + methodName + " not found."));
            return null;
        }

        if (method.getParameters().size() != node.getNumChildren()-1) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Method " + methodName + " expects " + method.getParameters().size() + " arguments."));
            return null;
        }

        for (int i = 1; i < node.getNumChildren(); i++) {
            Type typeArg = visit(node.getJmmChild(i), reports);
            if (typeArg == null) {
                reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Method " + methodName + " expects " + method.getParameters().size() + " arguments."));
                return null;
            }

            if (typeArg.equals(new Type("import", false))) {
                continue;
            }

            if (!typeArg.equals(method.getParameters().get(i-1).getType())) {
                reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Method " + methodName + " expects " + method.getParameters().get(i-1).getType() + " as argument " + (i-1) + ", but it received :" + typeArg + "."));
                return null;
            }
        }

        return method.getReturnType();
    }

    private Type visitArrayAccess(JmmNode node, List<Report> reports) {
        Type type = visit(node.getJmmChild(0), reports);
        Type indexType = visit(node.getJmmChild(1), reports);

        if (type == null || indexType == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation array access."));
            return null;
        }

        if (type.isArray() && (indexType.equals(new Type("int", false)) || indexType.equals(new Type("import", false)))) {
            return new Type(type.getName(), false);
        }

        reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation array access."));
        return null;
    }

    private Type visitArrayLength(JmmNode node, List<Report> reports) {
        Type type = visit(node.getJmmChild(0), reports);

        if (type == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation array.length."));
            return null;
        }

        if (type.isArray()) {
            return new Type("int", false);
        }

        reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation array.length."));
        return null;
    }


    private Type visitNewIntArray(JmmNode node, List<Report> reports) {
        Type type = visit(node.getJmmChild(0), reports);

        if (type == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation new int[]."));
            return null;
        }

        if (type.equals(new Type("int", false)) || type.equals(new Type("import", false))) {
            return new Type("int", true);
        }

        reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation new int[]."));
        return null;
    }

    // TODO: Check imports here as well
    private Type visitNewObject(JmmNode node, List<Report> reports) {
        String className = node.get("className");

        if (symbolTable.getClassName().equals(className) || symbolTable.getSuper().equals(className)) {
            return new Type(className, false);
        } else if (symbolTable.getImportsLastName().contains(className)) {
            return new Type(className, false);
        }

        return null;
    }

    private Type visitUnaryOp(JmmNode node, List<Report> reports) {
        Type type = visit(node.getJmmChild(0), reports);
        node.put("expectedType", "boolean");

        if (type == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation !."));
            return null;
        }

        if (type.equals(new Type("boolean", false)) || type.equals(new Type("import", false))) {
            return new Type("boolean", false);
        }

        reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation !."));
        return null;
    }

    private Type visitBinaryOp(JmmNode node, List<Report> reports) {
        String operation = node.get("op");
        Type leftType = visit(node.getJmmChild(0), reports);
        Type rightType = visit(node.getJmmChild(1), reports);

        if (leftType == null || rightType == null) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation " + operation + "."));
            return null;
        }


        switch (operation) {
            case "+":
            case "-":
            case "*":
            case "/":
                if (
                        (leftType.equals(new Type("int", false)) || leftType.equals(new Type("import", false))) &&
                        (rightType.equals(new Type("int", false)) || rightType.equals(new Type("import", false)))
                ) {
                    node.put("expectedType", "int");
                    return new Type("int", false);
                }
                break;
            case "<":
                if (
                        (leftType.equals(new Type("int", false)) || leftType.equals(new Type("import", false))) &&
                        (rightType.equals(new Type("int", false)) || rightType.equals(new Type("import", false)))
                ) {
                    node.put("expectedType", "int");
                    return new Type("boolean", false);
                }
                break;
            case "&&":
                if (
                        (leftType.equals(new Type("boolean", false)) || leftType.equals(new Type("import", false))) &&
                        (rightType.equals(new Type("boolean", false)) || rightType.equals(new Type("import", false)))
                ) {
                    node.put("expectedType", "boolean");
                    return new Type("boolean", false);
                }
                break;
        }

        reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Incompatible types for operation " + operation + "."));
        return null;
    }


    private Type visitIdentifier(JmmNode node, List<Report> reports) {
        return getTypeOfID(node.get("value"), reports);
    }

    private Type visitInteger(JmmNode node, List<Report> reports) {
        return new Type("int", false);
    }

    private Type visitThis(JmmNode node, List<Report> reports) {
        JmmSymbolTableMethod method = symbolTable.getMethodsMap().get(this.methodName);
        if (method.getStatic()) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Cannot use this in a static context."));
            return null;
        }
        return new Type(symbolTable.getClassName(), false);
    }

    private Type visitTrue(JmmNode node, List<Report> reports) {
        return new Type("boolean", false);
    }

    private Type visitFalse(JmmNode node, List<Report> reports) {
        return new Type("boolean", false);
    }

    private Type defaultVisit(JmmNode node, List<Report> reports) {
        for (JmmNode child : node.getChildren()) {
            visit(child, reports);
        }
        return null;
    }

    // TODO: Don't forget to check in the imports
    private Type getTypeOfID(String idName, List<Report> reports) {

        Boolean foundInLocal = localVariablesInitialized.get(idName);
        if (foundInLocal != null && foundInLocal) {
            return symbolTable.getMethodsMap().get(methodName).getLocalVariable(idName).getType();

        } else if (symbolTable.getMethodsMap().get(methodName).getParameter(idName) != null) {
            return symbolTable.getMethodsMap().get(methodName).getParameter(idName).getType();

        } else if (symbolTable.getField(idName) != null) {
            if (symbolTable.getMethodsMap().get(methodName).getStatic()) {
                reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Cannot access non-static variable " + idName + " from a static context."));
                return null;
            }
            return symbolTable.getField(idName).getType();

        } else if (symbolTable.getImportsLastName().contains(idName)) {
            return new Type(idName, false);
        } else {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "Variable " + idName + " not declared."));
        }

        return null;
    }
}
