package my.custom.packagee;

import com.github.dandelion.datatables.core.asset.Parameter;
import com.github.dandelion.datatables.core.constants.DTConstants;
import com.github.dandelion.datatables.core.exception.ExtensionLoadingException;
import com.github.dandelion.datatables.core.extension.AbstractExtension;
import com.github.dandelion.datatables.core.html.HtmlTable;

public class MyOtherCustomFeature extends AbstractExtension {

	@Override
	public String getName() {
		return "myOtherCustomFeature";
	}

	@Override
	public void setup(HtmlTable table) throws ExtensionLoadingException {
		addParameter(new Parameter(DTConstants.DT_AUTO_WIDTH, true));
	}
}