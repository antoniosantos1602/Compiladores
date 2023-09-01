package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class JmmSemanticTypeCheck extends AJmmVisitor<JmmSymbolTable, Boolean> {

    private List<Report> reports = new ArrayList<>();

    public List<Report> getReports() {
        return this.reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Method", this::visitMethod);
        addVisit("MainMethod", this::visitMainMethod);

        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitMainMethod(JmmNode node, JmmSymbolTable symbolTable) {
        if (!node.get("firstArgumentType").equals("String")) {
            reports.add(new Report( ReportType.ERROR, Stage.SEMANTIC, /*Line*/-1, /*Col*/-1, "First Argument in main must be of type String and it's currently " + node.get("firstArgumentType")));
            return true;
        }
        JmmSemanticMethodCheck jmmSemanticMethodCheck = new JmmSemanticMethodCheck(symbolTable, "main");
        jmmSemanticMethodCheck.visit(node, this.reports);
        return true;
    }

    private Boolean visitMethod(JmmNode node, JmmSymbolTable symbolTable) {
        JmmSemanticMethodCheck jmmSemanticMethodCheck = new JmmSemanticMethodCheck(symbolTable, node.get("methodName"));
        jmmSemanticMethodCheck.visit(node, this.reports);
        return true;
    }


    public Boolean defaultVisit(JmmNode node, JmmSymbolTable symbolTable) {
        for (JmmNode child : node.getChildren()) {
            visit(child, symbolTable);
        }
        return true;
    }





}
