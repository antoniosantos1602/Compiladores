package pt.up.fe.comp2023.ollir;

import com.sun.source.doctree.SystemPropertyTree;
//import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.analysis.JmmSymbol;
import pt.up.fe.comp2023.analysis.JmmSymbolTable;

import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Boolean, String> {

    private final StringBuilder ollirCode;
    public final JmmSymbolTable symbolTable;

    public int counter;
    public int counterIf;


    public OllirGenerator(JmmSymbolTable symbolTable) {
        this.ollirCode = new StringBuilder();
        this.symbolTable = symbolTable;
        this.counter = 0;
        this.counterIf = 0;

    }

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::defaultVisit);
        addVisit("Program", this::visitProgram);
        addVisit("Class", this::visitClass);
        addVisit("Method", this::visitMethod);
        addVisit("MainMethod", this::visitMainMethod);
        addVisit("LocalVar", this::visitLocalVar);
        //addVisit("Block", this::visitBlock);
        addVisit("IfElse", this::visitIfElse);
        addVisit("While", this::visitWhile);
        addVisit("ExprStmt", this::visitExprStmt);
        addVisit("Assignment", this::visitAssignment);
        addVisit("ArrayAssignment", this::visitArrayAssignment);
        addVisit("ParenExpr", this::visitParenExpr);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("ArrayLength", this::visitArrayLength);
        addVisit("ReturnExpr", this::visitReturnExpr);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("UniaryOp", this::visitUniaryOp);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("NewIntArray", this::visitNewIntArray);
        addVisit("NewObject", this::visitNewObject);
        addVisit("Integer", this::visitInteger);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("True", this::visitTrue);
        addVisit("False", this::visitFalse);
        addVisit("This", this::visitThis);


    }



    private String makeTemp(String type) {
        return "t" + counter++ + "." + type;
    }


    private String visitReturnExpr(JmmNode node, Boolean ok) {
        return visit(node.getChildren().get(0));
    }


    private String defaultVisit(JmmNode node, Boolean ok) {
        for (var child : node.getChildren()) {
            visit(child);
        }
        return null;
    }


    public String getOllir() {
        return ollirCode.toString();
    }

    private String visitProgram(JmmNode node, Boolean ok) {

        for (var importString : symbolTable.getImports()) {
            ollirCode.append("import ").append(importString).append(";\n");
        }

        for (var child : node.getChildren()) {
            visit(child);
        }

        return null;
    }


    private String visitClass(JmmNode node, Boolean ok) {
        ollirCode.append(symbolTable.getClassName());
        if (symbolTable.hasSuper()){
            ollirCode.append(" extends ").append(symbolTable.getSuper()).append(" {\n");
        } else {
            ollirCode.append(" {\n");
        }

        for (var field : symbolTable.getFields()) {
            ollirCode.append(".field private ");
            ollirCode.append(field.getName()).append(".");


            if (field.getType().isArray()) {
                ollirCode.append("array.");
                ollirCode.append(OllirUtils.getOllirType(field.getType().getName()));
            } else {
                ollirCode.append(OllirUtils.getOllirType(field.getType().getName()));
            }

            ollirCode.append(";\n");
        }

        ollirCode.append(".construct ").append(symbolTable.getClassName());ollirCode.append("().V {\n");
        ollirCode.append("\tinvokespecial(this, \"<init>\").V;\n");
        ollirCode.append("}\n\n");

        for (var child : node.getChildren()) {
            visit(child);
        }

        ollirCode.append("}\n");
        return null;
    }



    private String visitMethod(JmmNode node, Boolean ok) {
        var methodName = node.get("methodName");
        symbolTable.currentMethod = methodName;
        ollirCode.append(".method public ").append(methodName).append("(");

        var parameters = symbolTable.getParameters(methodName);

        //this will iterate through the parameters, get their TAC equivalents, and add them to paramenterCode, a big string containing comma separated arguments of the method
        var parametersCode = parameters.stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append(parametersCode);
        ollirCode.append(").");
        ollirCode.append(OllirUtils.getCode(symbolTable.getReturnType(methodName)));
        ollirCode.append(" {\n");

        for (int i = 0; i < node.getChildren().size() - 1; i++) {
            visit(node.getChildren().get(i));
        }

        String childReturn = visit(node.getChildren().get(node.getChildren().size() - 1));
        if (childReturn != null) {
            ollirCode.append("ret.");
            ollirCode.append(OllirUtils.getCode(symbolTable.getReturnType(methodName)));
            ollirCode.append(" ").append(childReturn);
            ollirCode.append(";\n");
        }else{
            ollirCode.append("ret.");
            ollirCode.append(OllirUtils.getCode(symbolTable.getReturnType(methodName)));
            ollirCode.append(";\n");
        }

        ollirCode.append("}\n");
        return null;
    }

    private String visitMainMethod(JmmNode node, Boolean ok) {
        var methodName = "main";
        symbolTable.currentMethod = methodName;
        ollirCode.append(".method public static main(");

        var parameters = symbolTable.getParameters(methodName);

        //this will iterate through the parameters, get their TAC equivalents, and add them to paramenterCode, a big string containing comma separated arguments of the method
        var parametersCode = parameters.stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append(parametersCode);
        ollirCode.append(").V");
        ollirCode.append(" {\n");

        for (var child: node.getChildren()){
            visit(child);
        }


        ollirCode.append("ret.V;\n");

        ollirCode.append("}\n");
        return null;
    }

    private String visitLocalVar(JmmNode node, Boolean ok) {
        String name = node.get("name");
        Type type = symbolTable.findType(name, node);
        String typeCode = OllirUtils.getCode(type);


        if (type.getName() == "int" || type.getName() == "boolean") {
            ollirCode.append(name).append(".").append(typeCode).append(" :=.").append(typeCode).append(" 0.").append(typeCode).append(";\n");
        } else {
            return null;
        }

        return null;
    }

    public String visitIfElse(JmmNode node, Boolean ok) {
        String condition = visit(node.getJmmChild(0));
        String ifLabel = "if_then_" + counterIf;
        String endLabel = "if_end_" + counterIf;
        counterIf++;

        ollirCode.append("if (").append(condition).append(") goto ").append(ifLabel).append(";\n");
        visit(node.getJmmChild(2));
        ollirCode.append("goto ").append(endLabel).append(";\n");
        ollirCode.append(ifLabel).append(":\n");

        visit(node.getJmmChild(1));
        ollirCode.append(endLabel).append(":\n");

        return null;
    }

    public String visitWhile(JmmNode node, Boolean ok) {
        String condition = visit(node.getJmmChild(0));
        String endLabel = "loop_end_" + counterIf;
        String loopLabel = "loop_" + counterIf;
        counterIf++;


        ollirCode.append(loopLabel).append(":\n");
        ollirCode.append("if (!.bool ").append(condition).append(") goto ").append(endLabel).append(";\n");

        visit(node.getJmmChild(1));
        ollirCode.append("goto ").append(loopLabel).append(";\n");
        ollirCode.append(endLabel).append(":\n");

        return null;
    }

    public String visitExprStmt(JmmNode node, Boolean ok) {
        String child = visit(node.getJmmChild(0));
        ollirCode.append(child).append(";\n");
        return null;
    }

    public String visitAssignment(JmmNode node, Boolean ok) {
        String name = node.get("var");
        String type = OllirUtils.getCode(symbolTable.findType(name, node));
        String value = visit(node.getJmmChild(0));
        JmmSymbolTable.Origin origin = symbolTable.findOrigin(name);
        StringBuilder sb = new StringBuilder();

        switch(origin) {
            case LOCAL:
                ollirCode.append(name).append(".").append(type).append(" :=.").append(type).append(" ").append(value).append(";\n");
                break;
            case PARAMETER:
                ollirCode.append("$").append(symbolTable.findParameterIndex(name)).append(".").append(name).append(".").append(type).append(" :=.").append(type).append(" ").append(value).append(";\n");
                break;
            case FIELD:
                ollirCode.append("putfield(this, ").append(name).append(".").append(type).append(", ").append(value).append(").").append(type).append(";\n");
                break;
            case IMPORT:
                ollirCode.append(name).append(".").append(type).append(" :=.").append(type).append(" ").append(value).append(";\n");
                break;
            default:
                break;
        }

        return sb.toString();
    }

    //dont trust this
    public String visitArrayAssignment(JmmNode node, Boolean ok) {
        String name = node.get("var");
        String type = OllirUtils.getCode(symbolTable.findType(name, node));
        String value = visit(node.getJmmChild(1));
        String index = visit(node.getJmmChild(0));

        JmmSymbolTable.Origin origin = symbolTable.findOrigin(name);



        switch(origin) {
            case PARAMETER:
                ollirCode.append("$").append(symbolTable.findParameterIndex(name)).append(".").append(name).append("[").append(index).append("].i32 :=.i32 ").append(value).append(";\n");
                break;
            default:
                ollirCode.append(name).append("[").append(index).append("].i32 :=.i32 ").append(value).append(";\n");
        }
        return null;
    }

    public String visitParenExpr(JmmNode node, Boolean ok) {
        return visit(node.getJmmChild(0));
    }

    public String visitArrayAccess(JmmNode node, Boolean ok) {
        String name = visit(node.getJmmChild(0));
        String type = OllirUtils.getCode(symbolTable.findType(name, node));
        String index = visit(node.getJmmChild(1));
        StringBuilder sb = new StringBuilder();

        String temp = makeTemp("i32");
        name = name.split("\\.")[name.contains("$") ? 1 : 0];
        JmmSymbolTable.Origin origin = symbolTable.findOrigin(name);
        if(name.matches("^t\\d+")) {
            origin = JmmSymbolTable.Origin.FIELD;
        }
        switch(origin) {
            case PARAMETER:
                ollirCode.append(temp).append(" :=.i32 ").append("$").append(symbolTable.findParameterIndex(name)).append(".").append(name).append("[").append(index).append("].i32").append(";\n");
                break;
            case FIELD:
                ollirCode.append(temp).append(" :=.i32 ").append(name).append(".array.i32").append("[").append(index).append("].i32;\n");
                break;
            default:
                ollirCode.append(temp).append(" :=.i32 ").append(name).append("[").append(index).append("].i32;\n");
        }


        /*
        if(name.contains("$")) {
            ollirCode.append(temp).append(" :=.i32 ").append(name).append(".").append(type).append("[").append(index).append("].i32;\n");
        } else {
            name = name.split("\\.")[0];
            ollirCode.append(temp).append(" :=.i32 ").append(name).append("[").append(index).append("].i32;\n");
        }
        */


        // name = name.replace(type, "");

        return temp;
    }
    /*
    //arraylength - a.length
*/
    public String visitArrayLength(JmmNode node, Boolean ok) {
        String name = visit(node.getJmmChild(0));
        StringBuilder sb = new StringBuilder();
        String temp = makeTemp("i32");
        ollirCode.append(temp).append(" :=.i32 arraylength(").append(name).append(").i32;\n");

        sb.append(temp);
        return sb.toString();
    }

    private String visitMethodCall(JmmNode node, Boolean ok) {
        String methodName = node.get("methodName");
        String calling = visit(node.getJmmChild(0));

        String type;
        boolean flagTemp = false;

        StringBuilder sb = new StringBuilder();

        boolean isImport = symbolTable.isImport(calling.split("\\.")[0]);

        String temp = "";

        if (isImport) {
            type = OllirUtils.getOllirType("void");
            sb.append("invokestatic(").append(node.getJmmChild(0).get("value")).append(", \"").append(methodName).append("\"");
        } else {
            if (symbolTable.getMethods().contains(methodName)){
                type = OllirUtils.getCode(symbolTable.getReturnType(methodName));
            } else {
                type = OllirUtils.getCode(symbolTable.findType(methodName, node));
            }
            if(!node.getJmmParent().getKind().equals("Method") && !node.getJmmParent().getKind().equals("MainMethod") && !node.getJmmParent().getKind().equals("ExprStmt")) {
                flagTemp = true;
                temp = makeTemp(type);
                sb.append(temp).append(" :=.").append(type).append(" ");
            }

            sb.append("invokevirtual(").append(calling).append(", \"").append(methodName).append("\"");
        }


        for (int i = 1; i < node.getChildren().size(); i++) {
            sb.append(", ");
            sb.append(visit(node.getChildren().get(i)));
        }

        sb.append(").").append(type);

        if(flagTemp) {
            ollirCode.append(sb).append(";\n");
            return temp;
        }

        return sb.toString();
    }

    public String visitUniaryOp(JmmNode node, Boolean ok) {
        String value = visit(node.getJmmChild(0));
        StringBuilder sb = new StringBuilder();

        String temp = makeTemp("bool");
        ollirCode.append(temp).append(" :=.bool ").append("!.bool ").append(value).append(";\n");

        sb.append(temp);
        return sb.toString();
    }

    private String visitBinaryOp(JmmNode node, Boolean ok) {
        String op = node.get("op");
        String left = visit(node.getJmmChild(0));
        String right = visit(node.getJmmChild(1));

        StringBuilder sb = new StringBuilder();
        String temp = "";
        if(op.equals("&&") || op.equals("<")) {
            temp = makeTemp("bool");
            ollirCode.append(temp).append(" :=.bool ").append(left).append(" ").append(op).append(".bool ").append(right).append(";\n");

        }else {
            temp = makeTemp("i32");
            ollirCode.append(temp).append(" :=.i32 ").append(left).append(" ").append(op).append(".i32 ").append(right).append(";\n");
        }
        sb.append(temp);
        return sb.toString();
    }



    private String visitNewIntArray(JmmNode node, Boolean ok) {
        String size = visit(node.getJmmChild(0));
        StringBuilder sb = new StringBuilder();
        sb.append("new(array, ").append(size).append(").array.i32");
        return sb.toString();
    }

    private String visitNewObject(JmmNode node, Boolean ok) {
        StringBuilder sb = new StringBuilder();
        String className = node.get("className");
        if(!node.getJmmParent().getKind().equals("Assignment")){
            String temp = makeTemp(className);
            ollirCode.append(temp).append(" :=.").append(className).append(" new(").append(className).append(").").append(className).append(";\n");
            ollirCode.append("invokespecial(").append(temp).append(", \"<init>\").V;\n");
            return temp.toString();
        } else {
            sb.append("new(").append(className).append(").").append(className);
        }

        return sb.toString();
    }


    private String visitInteger(JmmNode node, Boolean ok)  {

        StringBuilder sb = new StringBuilder();

        boolean isArray = node.getJmmParent().hasAttribute("isArray");

        if (isArray) {
            String temp = makeTemp("i32");
            sb.append(temp).append(" :=.i32 ").append(node.get("value")).append(".i32\n");
        }else{
            sb.append(node.get("value")).append(".i32");
        }

        return sb.toString();
    }


    private String visitIdentifier(JmmNode node, Boolean ok) {
        String name = node.get("value");
        StringBuilder sb = new StringBuilder();
        String type = OllirUtils.getCode(symbolTable.findType(name, node));
        JmmSymbolTable.Origin origin = symbolTable.findOrigin(name);


        switch(origin) {
            case LOCAL:
                sb.append(name).append(".").append(type);
                break;
            case PARAMETER:
                sb.append("$").append(symbolTable.findParameterIndex(name)).append(".").append(name).append(".").append(type);
                break;
            case FIELD:
                String temp= makeTemp(type);
                ollirCode.append(temp).append(" :=.").append(type).append(" getfield(this, ").append(name).append(".").append(type).append(").").append(type).append(";\n");
                sb.append(temp);
                break;
            case IMPORT:
                sb.append(name).append(".").append(type);
                break;
        }


        return sb.toString();
    }


    private String visitTrue(JmmNode node, Boolean ok) {
        StringBuilder sb = new StringBuilder();
        sb.append("1").append(".bool");

        return sb.toString();
    }

    private String visitFalse(JmmNode node, Boolean ok) {
        StringBuilder sb = new StringBuilder();
        sb.append("0").append(".bool");

        return sb.toString();
    }

    private String visitThis(JmmNode node, Boolean ok) {
        String parentName = node.getJmmParent().get("methodName");
        String type = OllirUtils.getCode(symbolTable.getReturnType(parentName));
        String temp = makeTemp(type);
        ollirCode.append(temp).append(" :=.").append(type).append(" $0.this.").append(type).append(";\n");
        return temp;

    }

/*
* fazer invoke special para a= new constructor
*
temp_2.Simple :=.Simple new(Simple).Simple;
invokespecial(temp_2.Simple,"<init>").V;
*
*
*
* assignment ver primeiro local, param, field, por essa ordem
*
* */






}
