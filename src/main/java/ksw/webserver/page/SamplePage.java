package ksw.webserver.page;

import ksw.servlet.VelocityPage;
import ksw.servlet.AppRequest;

/**
 */
public class SamplePage extends VelocityPage
{
    public static final String PageName = "sample";

    private String _flavor;

    @Override
    public void start (AppRequest request)
    {
        super.start(request);
        _flavor = request.getParameter("flavor");
        if (_flavor == null) {
            _flavor = "vacuum";
        }
    }

    public String getTitle()
    {
        return "Testing";
    }

    public String getFlavor()
    {
        return _flavor;
    }
}
