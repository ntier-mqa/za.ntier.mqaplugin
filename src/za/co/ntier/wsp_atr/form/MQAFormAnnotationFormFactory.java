package za.co.ntier.wsp_atr.form;

import org.adempiere.webui.factory.AnnotationBasedFormFactory;
import org.adempiere.webui.factory.IFormFactory;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = IFormFactory.class, property = { "service.ranking:Integer=1" })
public class MQAFormAnnotationFormFactory extends AnnotationBasedFormFactory {
	@Override
	protected String[] getPackages() {
		return new String[] { "za.co.ntier.wsp_atr.form" };
	} 
}
