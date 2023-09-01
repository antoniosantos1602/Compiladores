package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {

    public String currentMethod = "";
    private List<String> imports = new ArrayList<>();
    private String className = "", superClass = "";
    private List<Symbol> fields = new ArrayList<>();
    private HashMap<String, JmmSymbolTableMethod> methods = new HashMap<>();
    public JmmSymbolTable() {}

    public JmmSymbolTable(String className, String superClass) {
        this.className = className;
        this.superClass = superClass;
    }

    public String getCurrentMethod() {
        return currentMethod;
    }
    @Override
    public List<String> getImports() {
        return this.imports;
    }

    public List<String> getImportsLastName() {
        List<String> importsLastName = new ArrayList<>();
        for (String importName : imports) {
            importsLastName.add(importName.substring(importName.lastIndexOf('.') + 1));
        }
        return importsLastName;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.superClass;
    }

    public Boolean hasSuper() {
        return !this.superClass.equals("");
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(this.methods.keySet());
    }

    public HashMap<String, JmmSymbolTableMethod> getMethodsMap() {
        return this.methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return this.methods.get(methodSignature).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return this.methods.get(methodSignature).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return this.methods.get(methodSignature).getLocalVariables();
    }

    public Type findType(String name, JmmNode node) {
        System.out.println("name: " + name);
        for (Symbol localVariable : this.methods.get(this.currentMethod).getLocalVariables()) {
            if (localVariable.getName().equals(name)) {
                return localVariable.getType();
            }
        }
        //System.out.println("Current method: " + this.currentMethod);
        //System.out.println("paramenters: " + this.methods.get(this.currentMethod).getParameters());
        for (Symbol parameter : this.methods.get(this.currentMethod).getParameters()) {
            System.out.println("name inside func " + name);
            if (parameter.getName().equals(name)) {
                return parameter.getType();
            }
        }

        for (Symbol field : this.fields) {
            if (field.getName().equals(name)) {
                return field.getType();
            }
        }

        for (String importName : this.imports) {
            if (importName.equals(name)) {
                return new Type("void", false);
            }
        }

        //ask johnny for expected type
        if(this.hasSuper()){
            return findExpectedType(node);
        }

        return new Type("void", false);
    }

    private Type findExpectedType(JmmNode node){
        Type type = null;
        JmmNode temp = node;
        while(type == null && temp.getJmmParent() != null){
            temp = temp.getJmmParent();
            String value = null;
            try{
                value = temp.get("expectedType");
            }
            catch (Exception ignored){}
            if(value != null){
                type = new Type(temp.get("expectedType"), false);
            }
        }
        if (type == null)
            type = new Type("void", false);
        return type;
    }

    public enum Origin {
        LOCAL, PARAMETER, FIELD, IMPORT, SUPER
    }

    public Origin findOrigin(String name) {
        for (Symbol localVariable : this.methods.get(this.currentMethod).getLocalVariables()) {
            if (localVariable.getName().equals(name)) {
                return Origin.LOCAL;
            }
        }

        for (Symbol parameter : this.methods.get(this.currentMethod).getParameters()) {
            if (parameter.getName().equals(name)) {
                return Origin.PARAMETER;
            }
        }

        for (Symbol field : this.fields) {
            if (field.getName().equals(name)) {
                return Origin.FIELD;
            }
        }

        for (String importName : this.imports) {
            if (importName.equals(name)) {
                return Origin.IMPORT;
            }
        }

        if(this.hasSuper()){
            return Origin.SUPER;
        }


        return null;
    }

    public int findParameterIndex(String name) {
        for (int i = 0; i < this.methods.get(this.currentMethod).getParameters().size(); i++) {
            if (this.methods.get(this.currentMethod).getParameters().get(i).getName().equals(name)) {
                if(!Objects.equals(this.currentMethod, "main")) {
                    return i+1;
                }
                else {
                    return i;
                }
            }
        }


        return -1;
    }

    public boolean isImport(String name){
        return this.imports.contains(name);
    }

    public Symbol getField(String name) {
        for (Symbol field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }

    public void setCurrentMethod(String currentMethod) {
        this.currentMethod = currentMethod;
    }

    public Boolean addImport(String importName) {
        this.imports.add(importName);
        return true;
    }

    public Boolean addClass(String className, String superClass) {
        this.className = className;
        this.superClass = superClass;
        return true;
    }

    public Boolean addField(Type type, String name, String value) {
        this.fields.add(new JmmSymbol(type, name, value));
        return true;
    }

    public Boolean addMethod(String name, JmmSymbolTableMethod symbolTableMethod) {
        this.methods.put(name, symbolTableMethod);
        return true;
    }

}
