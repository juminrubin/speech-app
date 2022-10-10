package org.jrtech.azure.speech.app;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;

public class FileUploadComposer extends GenericForwardComposer<Window> {

	public void doAfterCompose(Window comp) throws Exception {
		super.doAfterCompose(comp);
	}

	public void onUpload$btn(UploadEvent e)// throws InterruptedException
	{
		if (e.getMedias() != null) {
			StringBuilder sb = new StringBuilder("You uploaded: \n");

			for (Media m : e.getMedias()) {
				sb.append(m.getName());
				sb.append(" (");
				sb.append(m.getContentType());
				sb.append(")\n");
				sb.append("File name: ");
				sb.append("\n'").append(m.getName()).append("'");
			}

			Messagebox.show(sb.toString());
		} else {
			Messagebox.show("You uploaded no files!");
		}
	}
}
