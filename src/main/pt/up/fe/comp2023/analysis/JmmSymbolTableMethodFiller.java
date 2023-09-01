package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class JmmSymbolTableMethodFiller extends AJmmVisitor<JmmSymbolTableMethod, Boolean> {

    private List<Report> reports = new ArrayList<>();
    @Override
    protected void buildVisitor() {
        addVisit("Method", this::visitMethodDeclaration);
        addVisit("MainMethod", this::visitMainMethodDeclaration);
        addVisit("LocalVar", this::visitLocalVarDeclaration);
        /*
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Integer", this::dealWithLiteral);
        addVisit("Identifier", this::dealWithLiteral);
        addVisit("ExprStmt", this::dealWithExprStmt);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("ParenExpr", this::dealWithParenExpr);
        */
        setDefaultVisit(this::visitDefault);
    }

    public List<Report> getReports() {
        return this.reports;
    }

    public Boolean visitMethodDeclaration(JmmNode methodDeclaration, JmmSymbolTableMethod symbolTableMethod) {

        // Get the return type
        Boolean isArray = false;
        String returnName = methodDeclaration.getJmmChild(0).get("typeName");

        try {
            methodDeclaration.getJmmChild(0).get("isArray");
            isArray = true;
        } catch (Exception e) {}


        // Get the parameters
        List<Symbol> parameters = new ArrayList<>();
        for (JmmNode parameter : methodDeclaration.getJmmChild(1).getChildren()) {
            String parameterName = parameter.get("name");
            String parameterType = parameter.getJmmChild(0).get("typeName");
            Boolean parameterIsArray = false;
            try {
                parameter.getJmmChild(0).get("isArray");
                parameterIsArray = true;
            } catch (Exception e) {}
            Type parameterTypeObject = new Type(parameterType, parameterIsArray);
            Symbol parameterSymbol = new Symbol(parameterTypeObject, parameterName);
            parameters.add(parameterSymbol);
        }

        // Store information in the symbol table
        symbolTableMethod.setReturnType(new Type(returnName, isArray));
        symbolTableMethod.setParameters(parameters);
        symbolTableMethod.setStatic(false);

        for (int i = 2; i < methodDeclaration.getNumChildren(); i++) {
            visit(methodDeclaration.getJmmChild(i), symbolTableMethod);
        }


        return true;
    }

    public Boolean visitMainMethodDeclaration(JmmNode mainMethodDeclaration, JmmSymbolTableMethod symbolTableMethod) {
        String methodName = mainMethodDeclaration.get("methodName");
        String returnType = mainMethodDeclaration.get("returnType");
        String firstArgumentType = mainMethodDeclaration.get("firstArgumentType");
        String firstArgumentName = mainMethodDeclaration.get("firstArgumentName");

        // Store information in the symbol table
        symbolTableMethod.setStatic(true);
        symbolTableMethod.setReturnType(new Type(returnType, false));
        symbolTableMethod.addParameter(new Symbol(new Type(firstArgumentType, true), firstArgumentName));

        for (JmmNode child : mainMethodDeclaration.getChildren()) {
            visit(child, symbolTableMethod);
        }

        return true;
    }


    public Boolean visitLocalVarDeclaration(JmmNode localVarDeclaration, JmmSymbolTableMethod symbolTableMethod) {

        String typeName = localVarDeclaration.getJmmChild(0).get("typeName");
        Boolean isArray = false;

        try {
            localVarDeclaration.getJmmChild(0).get("isArray");
            isArray = true;
        } catch (Exception e) {}

        symbolTableMethod.addLocalVariable(new Symbol(new Type(typeName, isArray), localVarDeclaration.get("name")));

        return true;
    }

    private Boolean dealWithAssignment(JmmNode jmmNode, JmmSymbolTableMethod symbolTable) {
        return true;
    }

    private Boolean dealWithLiteral(JmmNode jmmNode, JmmSymbolTableMethod symbolTable) {
        return true;
    }

    private Boolean dealWithExprStmt(JmmNode jmmNode, JmmSymbolTableMethod symbolTable) {
        return true;
    }

    private Boolean dealWithBinaryOp(JmmNode jmmNode, JmmSymbolTableMethod symbolTable) {
        return true;
    }

    private Boolean dealWithParenExpr(JmmNode jmmNode, JmmSymbolTableMethod symbolTable) {
        return true;
    }

    private Boolean visitDefault(JmmNode jmmNode, JmmSymbolTableMethod symbolTable) {
        return true;
    }

}
