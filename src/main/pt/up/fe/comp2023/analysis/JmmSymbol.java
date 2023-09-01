package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class JmmSymbol extends Symbol {
    String value = null;
    public JmmSymbol(Type type, String name, String value) {
        super(type, name);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isInstantiated() {
        return value != null;
    }



}
