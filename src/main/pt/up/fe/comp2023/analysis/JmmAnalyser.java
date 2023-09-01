package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JmmAnalyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        JmmSymbolTable symbolTable = new JmmSymbolTable();
        JmmSymbolTableFiller jmmSymbolTableFiller = new JmmSymbolTableFiller();
        jmmSymbolTableFiller.visit(jmmParserResult.getRootNode(), symbolTable);

        List<Report> reports = jmmSymbolTableFiller.getReports();

        // In here do the semantic analysis of the code
        JmmSemanticTypeCheck jmmSemanticTypeCheck = new JmmSemanticTypeCheck();
        jmmSemanticTypeCheck.visit(jmmParserResult.getRootNode(), symbolTable);
        reports.addAll(jmmSemanticTypeCheck.getReports());

        // Print the symbol table of each method
        /* HashMap<String, JmmSymbolTableMethod> methods = symbolTable.getMethodsMap();

        for (String method : methods.keySet()) {
            JmmSymbolTableMethod methodInfo = methods.get(method);
            System.out.println("--------------- Method NAME: " + method + " ---------------");
            System.out.println("Return: " + methodInfo.getReturnType());
            System.out.println("Parameters: " + methodInfo.getParameters());
            System.out.println("Local Variables: " + methodInfo.getLocalVariables());
            System.out.println("Static: " + methodInfo.getStatic());
            System.out.println("----------------------------------------------------------");
        }
        */


        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
