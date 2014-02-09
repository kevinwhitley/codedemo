package ksw.servlet;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;

import java.io.PrintWriter;

/**
 * Write out a page using Velocity.
 */
public abstract class VelocityPage extends ServletPage
{
    @Override
    public void write(PrintWriter writer)
    {
        VelocityContext context = new VelocityContext();
        context.put("page", this);
        try {
            Template template = getTemplate();
            if (template != null) {
                template.merge(context, writer);
            }
        }
        catch (Exception exc) {
            System.out.println("Exception writing page in VelocityPage " + exc);
        }
    }

    private Template getTemplate()
    {
        // we'll get the template associated with the class, walking up class hierarchy if needed
        // that gets terminated at VelocityPage
        Class pageClass = getClass();
        Class velocityPageClass = VelocityPage.class;
        Template template = null;
        while (pageClass != velocityPageClass) {
            String className = pageClass.getName();
            String templateName = className.replace('.', '/') + ".vm";
            if (Velocity.resourceExists(templateName)) {
                try {
                    template = Velocity.getTemplate(templateName);
                    // success
                    return template;
                }
                catch (Exception exc) {
                    // shouldn't happen, since we've checked already
                    // any exception means that we couldn't get the template, continue
                }

            }
            // if we're here, we weren't able to find the template, go up to superclass
            pageClass = pageClass.getSuperclass();
        }

        // if we're here, there is no template for the class
        System.out.println("No template for page class " + getClass().getSimpleName());
        return null;
    }
}
