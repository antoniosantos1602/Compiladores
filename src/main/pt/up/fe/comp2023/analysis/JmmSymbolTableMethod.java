package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class JmmSymbolTableMethod {
    private Type returnType;
    private List<Symbol> parameters;
    private List<Symbol> localVariables;
    private Boolean isStatic;

    public JmmSymbolTableMethod() {
        this.returnType = new Type("", false);
        this.parameters = new ArrayList<>();
        this.localVariables = new ArrayList<>();
        this.isStatic = false;
    }

    public JmmSymbolTableMethod(Type returnType, List<Symbol> parameters, List<Symbol> localVariables, Boolean isStatic) {
        this.returnType = returnType;
        this.parameters = parameters;
        this.localVariables = localVariables;
        this.isStatic = isStatic;
    }


    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public void setParameters(List<Symbol> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Symbol parameter) {
        this.parameters.add(parameter);
    }

    public void addLocalVariable(Symbol localVariable) {
        this.localVariables.add(localVariable);
    }

    public void setStatic(Boolean isStatic) {
        this.isStatic = isStatic;
    }
    public Type getReturnType() {
        return returnType;
    }
    public List<Symbol> getParameters() {
        return parameters;
    }

    public Symbol getParameter(String name) {
        for (Symbol parameter : parameters) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        return null;
    }
    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    public Symbol getLocalVariable(String name) {
        for (Symbol localVariable : localVariables) {
            if (localVariable.getName().equals(name)) {
                return localVariable;
            }
        }
        return null;
    }

    public Boolean getStatic() {
        return isStatic;
    }

}