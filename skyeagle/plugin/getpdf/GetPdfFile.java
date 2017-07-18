package skyeagle.plugin.getpdf;

import java.io.File;

import skyeagle.plugin.gui.UpdateDialog;

public interface GetPdfFile {

	public void getFile(UpdateDialog dig,File dir,Boolean usingProxy);

}
