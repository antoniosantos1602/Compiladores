package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.analysis.JmmSymbolTable;

import java.util.Collections;

public class Ollir implements JmmOptimization {


    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (semanticsResult.getConfig().getOrDefault("optimize", "false").equals("false")) {
            return semanticsResult;
        }
        FoldingVisitor foldingVisitor = new FoldingVisitor();
        PropagationVisitor propagationVisitor = new PropagationVisitor(semanticsResult.getSymbolTable());
         do {
            foldingVisitor.resetUpdated();
            propagationVisitor.resetUpdated();
            foldingVisitor.visit(semanticsResult.getRootNode(), true);
            propagationVisitor.visit(semanticsResult.getRootNode(), true);
             System.out.println("Folding: " + foldingVisitor.getUpdated());
             System.out.println("Propagation: " + propagationVisitor.getUpdated());
        } while(foldingVisitor.getUpdated() || propagationVisitor.getUpdated() );


        return semanticsResult;
    }


    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        OllirGenerator ollirGenerator = new OllirGenerator((JmmSymbolTable) jmmSemanticsResult.getSymbolTable());

        ollirGenerator.visit(jmmSemanticsResult.getRootNode());

        String code = ollirGenerator.getOllir();

        System.out.println(code);

        return new OllirResult(jmmSemanticsResult, code, Collections.emptyList());
    }



}
