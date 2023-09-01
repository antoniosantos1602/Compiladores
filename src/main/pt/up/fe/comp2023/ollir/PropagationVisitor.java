package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.analysis.JmmSymbolTable;
import pt.up.fe.comp2023.analysis.JmmSymbolTableMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PropagationVisitor extends AJmmVisitor<Boolean, String> {

    private HashMap<String, String>  variables;
    private SymbolTable symbolTable;

    private boolean updated;
    private boolean branching;

    public PropagationVisitor(SymbolTable symbolTable) {
        variables = new HashMap<>();
        this.symbolTable = symbolTable;
        this.updated = false;
        this.branching = false;
    }

    @Override
    protected void buildVisitor(){
        addVisit("Method", this::visitMethod);
        addVisit("MainMethod", this::visitMainMethod);
        addVisit("IfElse", this::visitIfElse);
        addVisit("While", this::visitWhile);
        addVisit("Assignment", this::visitAssignment);
        addVisit("Identifier", this::visitIdentifier);

        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode node, Boolean ok) {
        for (var child : node.getChildren()) {
            visit(child);
        }
        return null;
    }

    public String visitMethod(JmmNode node, Boolean ok) {
        variables = new HashMap<>();
        List<Symbol> methodVariables = symbolTable.getLocalVariables(node.get("methodName"));

        for (var symbol: methodVariables) {
            variables.put(symbol.getName(), null);
        }

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;

    }
    public String visitMainMethod(JmmNode node, Boolean ok) {
        variables = new HashMap<>();
        List<Symbol> methodVariables = symbolTable.getLocalVariables(node.get("methodName"));

        for (var symbol: methodVariables) {
            variables.put(symbol.getName(), null);
        }

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;

    }

    public String visitIfElse(JmmNode node, Boolean ok) {
        HashMap<String, String> originVariables = new HashMap<>(this.variables);
        HashMap<String, String> filterVariables = new HashMap<>(this.variables);

       
        visit(node.getChildren().get(0));

        for (var i=1; i<3; i++){
            visit(node.getChildren().get(i));
            for(var key : this.variables.keySet()){
                if(filterVariables.containsKey(key) && filterVariables.get(key) != null){
                    if(!filterVariables.get(key).equals(this.variables.get(key))){
                        filterVariables.put(key, null);
                    }
                }
            }
            this.variables = new HashMap<>(originVariables);
        }

        this.variables = new HashMap<>(filterVariables);
        
        return null;
    }

    public String visitWhile(JmmNode node, Boolean ok) {
        this.branching = true;
        for (var child : node.getChildren()) {
            visit(child);
        }
        this.branching = false;
        return null;
    }

    public String visitAssignment(JmmNode node, Boolean ok) {
        String name = node.get("var");
        JmmNode child = node.getChildren().get(0);
        visit(child);
        String childKind = child.getKind();
        boolean isInteger = childKind.equals("Integer");
        boolean isBool = childKind.equals("True") || childKind.equals("False");

        if(branching){
            variables.put(name, null);
            return null;
        }
        if (isInteger) {
            variables.put(name, child.get("value"));
        }else if (isBool) {
            variables.put(name, child.getKind().equals("True") ? "true" : "false");
        }else{
            variables.put(name, null);
        }


        return null;
    }

    public String visitIdentifier(JmmNode node, Boolean ok) {
        String name = node.get("value");
        String value = variables.get(name);
        if (value != null) {
            JmmNodeImpl newNode;
            if(value.equals("true") || value.equals("false")){
                newNode = new JmmNodeImpl("Boolean");
                node.replace(newNode);
                this.updated = true;
            }else {
                newNode = new JmmNodeImpl("Integer");
                newNode.put("value", value);
                node.replace(newNode);
                this.updated = true;
            }
        }
        return null;
    }

    public boolean getUpdated() {
        return this.updated;
    }

    public void resetUpdated() {
        this.updated = false;
    }
}















