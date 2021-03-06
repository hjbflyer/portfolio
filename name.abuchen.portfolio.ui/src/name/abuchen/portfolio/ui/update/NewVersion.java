package name.abuchen.portfolio.ui.update;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.osgi.framework.Version;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/* package */class NewVersion
{
    private static final String VERSION_MARKER = "-- "; //$NON-NLS-1$

    /* package */ static class Release
    {
        private Version version;
        private List<String> lines = new ArrayList<>();
        private List<ConditionalMessage> messages = new ArrayList<>();

        public Release(Version version)
        {
            this.version = version;
        }

        public Version getVersion()
        {
            return version;
        }

        public List<String> getLines()
        {
            return lines;
        }

        public List<ConditionalMessage> getMessages()
        {
            return messages;
        }
    }

    /* package */ static class Expression
    {
        private String property;
        private Pattern pattern;

        public Expression(String property, String pattern)
        {
            this.property = property;
            this.pattern = Pattern.compile(pattern);
        }

        public boolean isApplicable()
        {
            return pattern.matcher(System.getProperty(property)).matches();
        }
    }

    /* package */ static class ConditionalMessage
    {
        private List<Expression> expressions = new ArrayList<>();
        private List<String> lines = new ArrayList<>();

        public ConditionalMessage(String condition)
        {
            String[] all = condition.substring(1, condition.length() - 1).split("&"); //$NON-NLS-1$
            for (String expr : all)
            {
                int p = expr.indexOf('=');
                if (p > 0)
                    this.expressions.add(new Expression(expr.substring(0, p), expr.substring(p + 1)));
                else
                    PortfolioPlugin.log(MessageFormat.format("Invalid update expression ''{0}'' in condition ''{1}''", //$NON-NLS-1$
                                    expr, condition));
            }

            if (this.expressions.isEmpty())
                throw new IllegalArgumentException(
                                MessageFormat.format("No update expressions found for ''{0}''", condition)); //$NON-NLS-1$
        }

        public boolean isApplicable()
        {
            for (Expression e : expressions)
                if (!e.isApplicable())
                    return false;

            return true;
        }

        public List<String> getLines()
        {
            return lines;
        }
    }

    private String version;
    private String minimumJavaVersionRequired;
    private List<Release> releases = new ArrayList<>();

    public NewVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersionHistory(String history)
    {
        if (history == null)
            return;

        String[] lines = history.split("\\r?\\n"); //$NON-NLS-1$

        Release release = new Release(null); // dummy
        ConditionalMessage conditionalMessage = null;
        for (String line : lines)
        {
            if (line.startsWith(VERSION_MARKER))
            {
                try
                {
                    Version v = new Version(line.substring(VERSION_MARKER.length()));
                    release = new Release(v);
                    conditionalMessage = null;
                    this.releases.add(release);
                }
                catch (IllegalArgumentException e)
                {
                    PortfolioPlugin.log(e);

                    // in case parsing of the version fails, let's setup a dummy
                    // release to not loose the update information
                    release = new Release(new Version(99, 0, 0));
                    this.releases.add(release);
                }
            }
            else if (line.startsWith("~~ (")) //$NON-NLS-1$
            {
                try
                {
                    String condition = line.substring(3);
                    conditionalMessage = new ConditionalMessage(condition);
                    release.messages.add(conditionalMessage);
                }
                catch (IllegalArgumentException | IndexOutOfBoundsException e)
                {
                    PortfolioPlugin.log(e);

                    // ignore -> lines will be added to regular release message
                }
            }
            else if (line.startsWith("~~")) //$NON-NLS-1$
            {
                String text = line.substring(Math.min(3, line.length()));
                if (conditionalMessage != null)
                    conditionalMessage.lines.add(text);
                else
                    release.lines.add(text);
            }
            else
            {
                release.lines.add(line);
            }
        }

        // sort reverse by version
        Collections.sort(releases, (r, l) -> l.version.compareTo(r.version));
    }

    public List<Release> getReleases()
    {
        return releases;
    }

    public void setMinimumJavaVersionRequired(String minimumJavaVersionRequired)
    {
        this.minimumJavaVersionRequired = minimumJavaVersionRequired;
    }

    public boolean requiresNewJavaVersion()
    {
        if (minimumJavaVersionRequired == null)
            return false;

        double current = Double.parseDouble(System.getProperty("java.specification.version")); //$NON-NLS-1$
        double required = Double.parseDouble(minimumJavaVersionRequired);

        return required > current;
    }
}
