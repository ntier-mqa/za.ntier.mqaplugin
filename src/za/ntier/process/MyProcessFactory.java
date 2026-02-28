package za.ntier.process;

import org.adempiere.base.AnnotationBasedProcessFactory;
import org.adempiere.base.IProcessFactory;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = IProcessFactory.class, property = {"service.ranking:Integer=100"}) 
public class MyProcessFactory extends AnnotationBasedProcessFactory {

	@Override
	protected String[] getPackages() {
		return new String[] {"za.ntier.process","za.ntier.report.fin","za.co.ntier.wf.process"
				,"za.co.ntier.wsp_atr.process","za.co.ntier.wsp_atr.report.process","za.co.ntier.bg"};
	}

}
