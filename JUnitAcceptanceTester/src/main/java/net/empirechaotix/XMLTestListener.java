package net.empirechaotix;

import junit.framework.Test;
import junit.framework.TestResult;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Main test driver.
 */
public class XMLTestListener
{

    /**
     * Wraps {@link Description} into {@link Test} enough to fake {@link JUnitResultFormatter}.
     */
    public static class DescriptionAsTest implements Test {
        private final Description description;

        public DescriptionAsTest(Description description) {
            this.description = description;
        }

        public int countTestCases() {
            return 1;
        }

        public void run(TestResult result) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@link JUnitResultFormatter} determines the test name by reflection.
         */
        public String getName() {
            return description.getDisplayName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DescriptionAsTest that = (DescriptionAsTest) o;

            if (!description.equals(that.description)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return description.hashCode();
        }
    }

    /**
     * Adopts {@link JUnitResultFormatter} into {@link RunListener},
     * and also captures stdout/stderr by intercepting the likes of {@link System#out}.
     *
     * Because Ant JUnit formatter uses one stderr/stdout per one test suite,
     * we capture each test case into a separate report file.
     */
    public static class JUnitResultFormatterAsRunListener extends RunListener {
        protected final JUnitResultFormatter formatter;
        private ByteArrayOutputStream stdout,stderr;
        private PrintStream oldStdout,oldStderr;
        private int problem;
        private long startTime;

        public JUnitResultFormatterAsRunListener(JUnitResultFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void testRunStarted(Description description) throws Exception {
        }

        @Override
        public void testRunFinished(Result result) throws Exception {
        }

        @Override
        public void testStarted(Description description) throws Exception {
            formatter.startTestSuite(new JUnitTest(description.getDisplayName()));
            formatter.startTest(new DescriptionAsTest(description));
            problem = 0;
            startTime = System.currentTimeMillis();

            this.oldStdout = System.out;
            this.oldStderr = System.err;
            System.setOut(new PrintStream(stdout = new ByteArrayOutputStream()));
            System.setErr(new PrintStream(stderr = new ByteArrayOutputStream()));
        }

        @Override
        public void testFinished(Description description) throws Exception {
            System.out.flush();
            System.err.flush();
            System.setOut(oldStdout);
            System.setErr(oldStderr);

            formatter.setSystemOutput(stdout.toString());
            formatter.setSystemError(stderr.toString());
            formatter.endTest(new DescriptionAsTest(description));

            JUnitTest suite = new JUnitTest(description.getDisplayName());
            suite.setCounts(1,problem,0);
            suite.setRunTime(System.currentTimeMillis()-startTime);
            formatter.endTestSuite(suite);
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            testAssumptionFailure(failure);
        }

        @Override
        public void testAssumptionFailure(Failure failure) {
            problem++;
            formatter.addError(new DescriptionAsTest(failure.getDescription()), failure.getException());
        }

        @Override
        public void testIgnored(Description description) throws Exception {
            super.testIgnored(description);
        }
    }
}