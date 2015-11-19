package Assessment;

import ptolemy.actor.gui.PtExecuteApplication;


public class Test {
    public static void main(String[] args) throws Exception {
        PtExecuteApplication ptExec = new PtExecuteApplication(new String[]{
                "\\\\userfs\\pk627\\w2k\\Desktop\\EMBS\\Assessment\\EMBS_OA1_2015_Ptolemy\\model.xml"
        });

        ptExec.runModels();
        ptExec.waitForFinish();
    }
}