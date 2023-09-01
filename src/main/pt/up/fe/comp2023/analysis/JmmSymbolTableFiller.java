package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class JmmSymbolTableFiller extends AJmmVisitor<JmmSymbolTable, Boolean> {

    private List<Report> reports = new ArrayList<>();
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("Import", this::dealWithImport);
        addVisit("Class", this::dealWithClass);
        addVisit("Field", this::dealWithVarDeclaration);
        addVisit("Method", this::dealWithMethod);
        addVisit("MainMethod", this::dealWithMainMethod);
        //setDefaultVisit(); to set a default "handler" of JmmNodes
    }

    public List<Report> getReports() {
        return this.reports;
    }

    public Boolean dealWithProgram(JmmNode program, JmmSymbolTable symbolTable) {

        for (JmmNode child : program.getChildren()) {
            visit(child, symbolTable);
        }
        return true;
    }

    public Boolean dealWithImport(JmmNode importDeclaration, JmmSymbolTable symbolTable) {
        String importFragments = importDeclaration.getChildren()
                .stream()
                .map(jmmNode -> jmmNode.get("id"))
                .reduce((s, s2) -> s + "." + s2).get();

        symbolTable.addImport(importFragments);
        return true;
    }

    public Boolean dealWithClass(JmmNode classDeclaration, JmmSymbolTable symbolTable) {

        try {
            symbolTable.addClass(classDeclaration.get("name"), classDeclaration.get("superclass"));
        } catch (Exception e) {
            symbolTable.addClass(classDeclaration.get("name"), "");
        }


        for (JmmNode child : classDeclaration.getChildren()) {
            visit(child, symbolTable);
        }

        return true;
    }


    public Boolean dealWithVarDeclaration(JmmNode varDeclaration, JmmSymbolTable symbolTable) {
        String typeName = varDeclaration.getJmmChild(0).get("typeName");
        Boolean isArray = false;

        try {
            varDeclaration.getJmmChild(0).get("isArray");
            isArray = true;
        } catch (Exception e) {}

        symbolTable.addField( new Type(typeName, isArray), varDeclaration.get("name"), "");

        return true;
    }

    public Boolean dealWithMethod(JmmNode methodDeclaration, JmmSymbolTable symbolTable) {
        String methodName = methodDeclaration.get("methodName");

        JmmSymbolTableMethod symbolTableMethod = new JmmSymbolTableMethod();
        JmmSymbolTableMethodFiller jmmSymbolTableMethodFiller = new JmmSymbolTableMethodFiller();
        jmmSymbolTableMethodFiller.visit(methodDeclaration, symbolTableMethod);
        symbolTableMethod.setStatic(false);


        symbolTable.addMethod(methodName, symbolTableMethod);
        this.reports.addAll(jmmSymbolTableMethodFiller.getReports());

        return true;
    }

    public Boolean dealWithMainMethod(JmmNode mainMethodDeclaration, JmmSymbolTable symbolTable) {

        String methodName = mainMethodDeclaration.get("methodName");

        JmmSymbolTableMethod symbolTableMethod = new JmmSymbolTableMethod();
        JmmSymbolTableMethodFiller jmmSymbolTableMethodFiller = new JmmSymbolTableMethodFiller();
        jmmSymbolTableMethodFiller.visit(mainMethodDeclaration, symbolTableMethod);


        symbolTable.addMethod(methodName, symbolTableMethod);
        this.reports.addAll(jmmSymbolTableMethodFiller.getReports());

        return true;
    }


}






