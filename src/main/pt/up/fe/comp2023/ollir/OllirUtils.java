package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirUtils {

    // this getCode make a whole string consisting of the symbol(attribute)'s name and types (and array if required)
    public static String getCode(Symbol symbol){
        return symbol.getName()+"."+getCode(symbol.getType());
    }

    // this getCode sees if the attribute type is an array and appends the required string to the string returned by getOllirType
    public static String getCode(Type type){
        StringBuilder ollirCode = new StringBuilder();

        if(type.isArray())
            ollirCode.append("array.");

        ollirCode.append(getOllirType(type.getName()));
        return ollirCode.toString();
    }

    // getOllirType gets the type in Jmm code, and translates it into TAC equivalent
    public static String getOllirType(String jmmType){
        var typeString = switch (jmmType) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "void" -> "V";
            default -> jmmType;
        };

        return typeString;
    }

    public static String getParentName(JmmNode node){

        while(!node.getKind().equals("MethodDeclaration") && !node.getKind().equals("ClassDeclaration") && !node.getKind().equals("MainClass"))
            node = node.getJmmParent();

        if (node.getKind().equals("MethodDeclaration") || node.getKind().equals("MainClass"))
            return node.get("name");

        return "main";
    }
}
