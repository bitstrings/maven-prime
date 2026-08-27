package org.bitstrings.idea.plugins.mavenprime.spy;

public final class SpyProtocol
{
    public static final String PORT_PROPERTY = "mavenprime.spy.port";

    public static final String TOKEN_PROPERTY = "mavenprime.spy.token";

    public static final String BUILD_PROPERTY = "mavenprime.spy.build";

    public static final String HANDSHAKE = "HANDSHAKE";

    public static final String EXT_CLASS_PATH_PROPERTY = "maven.ext.class.path";

    public static final String SEPARATOR = "\t";

    public static final String SESSION_STARTED = "SESSION_STARTED";

    public static final String SESSION_ENDED = "SESSION_ENDED";

    public static final String PROJECT_STARTED = "PROJECT_STARTED";

    public static final String PROJECT_SUCCEEDED = "PROJECT_SUCCEEDED";

    public static final String PROJECT_FAILED = "PROJECT_FAILED";

    public static final String PROJECT_SKIPPED = "PROJECT_SKIPPED";

    public static final String MOJO_STARTED = "MOJO_STARTED";

    public static final String MOJO_FAILED = "MOJO_FAILED";

    public static final String MOJO_PROBLEM = "MOJO_PROBLEM";

    public static final String PROJECT_TIMING = "PROJECT_TIMING";

    public static final String MOJO_TIMING = "MOJO_TIMING";

    public static final String REACTOR_EDGE = "REACTOR_EDGE";

    public static final String ARTIFACT_DOWNLOADING = "ARTIFACT_DOWNLOADING";

    public static final String ARTIFACT_DOWNLOADED = "ARTIFACT_DOWNLOADED";

    public static final String METADATA_DOWNLOADED = "METADATA_DOWNLOADED";

    public static final String ARTIFACT_FAILED = "ARTIFACT_FAILED";

    public static final String METADATA_FAILED = "METADATA_FAILED";

    public static final String MODEL_DEPENDENCY = "MODEL_DEPENDENCY";

    public static final String MODEL_PLUGIN = "MODEL_PLUGIN";

    public static final String MODEL_PROPERTY = "MODEL_PROPERTY";

    public static final String DROPPED = "DROPPED";

    private SpyProtocol()
    {
    }

    public static String encode(String... fields)
    {
        StringBuilder line = new StringBuilder();

        for (String field : fields)
        {
            if (line.length() > 0)
            {
                line.append(SEPARATOR);
            }

            line.append(sanitize(field));
        }

        return line.toString();
    }

    public static String[] decode(String line)
    {
        return (line == null) ? new String[0] : line.split(SEPARATOR, -1);
    }

    private static String sanitize(String field)
    {
        if (field == null)
        {
            return "";
        }

        return field.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }
}
