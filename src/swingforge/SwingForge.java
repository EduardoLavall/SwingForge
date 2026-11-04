package swingforge;

import javax.swing.SwingUtilities;
import swingforge.ui.Light;
import swingforge.ui.SwingForgeApp;
import swingforge.core.Generator;
import swingforge.core.Spec;
import java.nio.file.Paths;

/** GUI padrao ou CLI para geracao em lote offline. */
public final class SwingForge {
    private SwingForge(){ }
    public static void main(String[] args) throws Exception {
        if(args.length==2 && "--demo".equals(args[0])){
            System.out.println("Arquivos gerados: "+Generator.generate(Spec.example(),Paths.get(args[1]).toAbsolutePath()));
            return;
        }
        Light.defaults();SwingUtilities.invokeLater(()->new SwingForgeApp().setVisible(true));
    }
}
