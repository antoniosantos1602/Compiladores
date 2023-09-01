package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.analysis.JmmSymbolTable;

import java.util.stream.Collectors;

public class FoldingVisitor extends AJmmVisitor<Boolean, String> {

    private boolean updated;

    public FoldingVisitor() {
        this.updated = false;
    }

    @Override
    protected void buildVisitor() {
        addVisit("ParenExpr", this::visitParenExpr);
        addVisit("UniaryOp", this::visitUniaryOp);
        addVisit("BinaryOp", this::visitBinaryOp);
        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode node, Boolean ok) {
        for (var child : node.getChildren()) {
            visit(child);
        }
        return null;
    }

    public String visitParenExpr(JmmNode node, Boolean ok) {
        JmmNode child = node.getChildren().get(0);
        String childKind = child.getKind();
        boolean isInteger = childKind.equals("Integer");
        boolean isBool = childKind.equals("True") || childKind.equals("False");


        JmmNodeImpl newNode = new JmmNodeImpl(childKind);
        if (isInteger) {
            newNode.put("value", child.get("value"));
            node.replace(newNode);
            this.updated = true;
        }else if (isBool) {
            newNode.put("value", child.getKind());
            node.replace(newNode);
            this.updated = true;
        }

        visit(child);
        return null;
    }

    public String visitUniaryOp(JmmNode node, Boolean ok) {
        System.out.println(node.getChildren());
        JmmNode child = node.getChildren().get(0);
        String childKind = child.getKind();
        boolean isBool = childKind.equals("True") || childKind.equals("False");

        if (isBool) {
            JmmNodeImpl newNode = new JmmNodeImpl(childKind);
            if(childKind.equals("True"))
                newNode = new JmmNodeImpl("True");
            else
                newNode = new JmmNodeImpl("False");
            node.replace(newNode);
            this.updated = true;
        }

        visit(child);

        return null;
    }
    private String visitBinaryOp(JmmNode node, Boolean ok) {
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);
        String leftKind = left.getKind();
        String rightKind = right.getKind();
        boolean isInteger = leftKind.equals("Integer") && rightKind.equals("Integer");
        boolean isBool = (leftKind.equals("True") || leftKind.equals("False")) && (rightKind.equals("True") || rightKind.equals("False"));

        String op = node.get("op");
        System.out.println("\n"+op+"\n");

        System.out.println(isBool);

        JmmNodeImpl newNode;
        if (isInteger || isBool) {
            switch (op) {
                case "+":
                    newNode = new JmmNodeImpl("Integer");
                    newNode.put("value", String.valueOf(Integer.parseInt(left.get("value")) + Integer.parseInt(right.get("value"))));
                    node.replace(newNode);
                    this.updated = true;
                    break;

                case "-":
                    newNode = new JmmNodeImpl("Integer");
                    newNode.put("value", String.valueOf(Integer.parseInt(left.get("value")) - Integer.parseInt(right.get("value"))));
                    node.replace(newNode);
                    this.updated = true;
                    break;

                case "*":
                    newNode = new JmmNodeImpl("Integer");
                    newNode.put("value", String.valueOf(Integer.parseInt(left.get("value")) * Integer.parseInt(right.get("value"))));
                    node.replace(newNode);
                    this.updated = true;
                    break;

                case "/":
                    newNode = new JmmNodeImpl("Integer");
                    newNode.put("value", String.valueOf(Integer.parseInt(left.get("value")) / Integer.parseInt(right.get("value"))));
                    node.replace(newNode);
                    this.updated = true;
                    break;

                case "<":
                    boolean value = Integer.parseInt(left.get("value")) < Integer.parseInt(right.get("value"));
                    if (value) {
                        newNode = new JmmNodeImpl("True");
                    } else {
                        newNode = new JmmNodeImpl("False");
                    }
                    node.replace(newNode);
                    this.updated = true;
                    break;

                case "&&":
                    boolean value2 = Boolean.parseBoolean(left.getKind()) && Boolean.parseBoolean(right.getKind());
                    System.out.println(Boolean.parseBoolean(left.getKind()) + " " + Boolean.parseBoolean(right.getKind()) + " " + value2);

                    if (value2) {
                        newNode = new JmmNodeImpl("True");
                    } else {
                        newNode = new JmmNodeImpl("False");
                    }
                    node.replace(newNode);
                    this.updated = true;
                    break;

                default:
                    break;
            }
        }
        visit(left);
        visit(right);

        return null;

    }

    public boolean getUpdated() {
        return this.updated;
    }

    public void resetUpdated() {
        this.updated = false;
    }
}
