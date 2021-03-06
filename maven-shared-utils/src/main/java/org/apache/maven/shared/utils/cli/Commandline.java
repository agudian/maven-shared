package org.apache.maven.shared.utils.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.maven.shared.utils.Os;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.cli.shell.BourneShell;
import org.apache.maven.shared.utils.cli.shell.CmdShell;
import org.apache.maven.shared.utils.cli.shell.CommandShell;
import org.apache.maven.shared.utils.cli.shell.Shell;

/**
 * <p/>
 * Commandline objects help handling command lines specifying processes to
 * execute.
 * </p>
 * <p/>
 * The class can be used to define a command line as nested elements or as a
 * helper to define a command line by an application.
 * </p>
 * <p/>
 * <code>
 * &lt;someelement&gt;<br>
 * &nbsp;&nbsp;&lt;acommandline executable="/executable/to/run"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 1" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument line="argument_1 argument_2 argument_3" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;argument value="argument 4" /&gt;<br>
 * &nbsp;&nbsp;&lt;/acommandline&gt;<br>
 * &lt;/someelement&gt;<br>
 * </code>
 * </p>
 * <p/>
 * The element <code>someelement</code> must provide a method
 * <code>createAcommandline</code> which returns an instance of this class.
 * </p>
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Commandline
    implements Cloneable
{
    private final List<Arg> arguments = new Vector<Arg>();

    //protected Vector envVars = new Vector();
    // synchronized added to preserve synchronize of Vector class
    private final Map<String,String> envVars = Collections.synchronizedMap( new LinkedHashMap<String,String>() );

    private Shell shell;

    /**
     * Create a new command line object.
     * Shell is autodetected from operating system
     */
    public Commandline( Shell shell )
    {
        this.shell = shell;
    }

    /**
     * Create a new command line object.
     * Shell is autodetected from operating system
     *
     * @param toProcess  The command to process
     */
    public Commandline( String toProcess )
    {
        setDefaultShell();
        String[] tmp = new String[0];
        try
        {
            tmp = CommandLineUtils.translateCommandline( toProcess );
        }
        catch ( Exception e )
        {
            System.err.println( "Error translating Commandline." );
        }
        if ( ( tmp != null ) && ( tmp.length > 0 ) )
        {
            setExecutable( tmp[0] );
            for ( int i = 1; i < tmp.length; i++ )
            {
                createArg().setValue( tmp[i] );
            }
        }
    }

    /**
     * Create a new command line object.
     * Shell is autodetected from operating system
     */
    public Commandline()
    {
        setDefaultShell();
    }

    /**
     * <p>Sets the shell or command-line interpretor for the detected operating system,
     * and the shell arguments.</p>
     */
    private void setDefaultShell()
    {
        //If this is windows set the shell to command.com or cmd.exe with correct arguments.
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            if ( Os.isFamily( Os.FAMILY_WIN9X ) )
            {
                setShell( new CommandShell() );
            }
            else
            {
                setShell( new CmdShell() );
            }
        }
        else
        {
            setShell( new BourneShell() );
        }
    }

    /**
     * Creates an argument object.
     * <p/>
     * <p>Each commandline object has at most one instance of the
     * argument class.  This method calls
     * <code>this.createArgument(false)</code>.</p>
     *
     * @return the argument object.
     */
    public Arg createArg()
    {
        return this.createArg( false );
    }

    /**
     * Creates an argument object and adds it to our list of args.
     * <p/>
     * <p>Each commandline object has at most one instance of the
     * argument class.</p>
     *
     * @param insertAtStart if true, the argument is inserted at the
     *                      beginning of the list of args, otherwise it is appended.
     */
    public Arg createArg( boolean insertAtStart )
    {
        Arg argument = new Argument();
        if ( insertAtStart )
        {
            arguments.add( 0, argument );
        }
        else
        {
            arguments.add( argument );
        }
        return argument;
    }

    /**
     * Sets the executable to run.
     */
    public void setExecutable( String executable )
    {
        shell.setExecutable( executable );
    }

    public String getExecutable()
    {

        return shell.getExecutable();
    }

    public void addArguments( String... line )
    {
        for ( String aLine : line )
        {
            createArg().setValue( aLine );
        }
    }

    /**
     * Add an environment variable
     */
    public void addEnvironment( String name, String value )
    {
        //envVars.add( name + "=" + value );
        envVars.put( name, value );
    }

    /**
     * Add system environment variables
     */
    public void addSystemEnvironment()
        throws Exception
    {
        Properties systemEnvVars = CommandLineUtils.getSystemEnvVars();

        for ( Object o : systemEnvVars.keySet() )
        {
            String key = (String) o;
            if ( !envVars.containsKey( key ) )
            {
                addEnvironment( key, systemEnvVars.getProperty( key ) );
            }
        }
    }

    /**
     * Return the list of environment variables
     */
    public String[] getEnvironmentVariables()
        throws CommandLineException
    {
        try
        {
            addSystemEnvironment();
        }
        catch ( Exception e )
        {
            throw new CommandLineException( "Error setting up environmental variables", e );
        }
        String[] environmentVars = new String[envVars.size()];
        int i = 0;
        for ( String name : envVars.keySet() )
        {
            String value = envVars.get( name );
            environmentVars[i] = name + "=" + value;
            i++;
        }
        return environmentVars;
    }

    /**
     * Returns the executable and all defined arguments.
     */
    public String[] getCommandline()
    {
        final String[] args = getArguments();
        String executable = getExecutable();

        if ( executable == null )
        {
            return args;
        }
        final String[] result = new String[args.length + 1];
        result[0] = executable;
        System.arraycopy( args, 0, result, 1, args.length );
        return result;
    }

    /**
     * Returns the shell, executable and all defined arguments.
     */
    private String[] getShellCommandline()
    {
        List<String> shellCommandLine = getShell().getShellCommandLine( getArguments() );
        return shellCommandLine.toArray( new String[shellCommandLine.size()] );
    }

    /**
     * Returns all arguments defined by <code>addLine</code>,
     * <code>addValue</code> or the argument object.
     */
    public String[] getArguments()
    {
        List<String> result = new ArrayList<String>( arguments.size() * 2 );
        for ( Arg argument : arguments )
        {
            Argument arg = (Argument) argument;
            String[] s = arg.getParts();
            if ( s != null )
            {
                Collections.addAll( result, s );
            }
        }

        return result.toArray( new String[result.size()] );
    }

    public String toString()
    {
        return StringUtils.join( getShellCommandline(), " " );
    }


    public Object clone()
    {
        throw new RuntimeException( "Do we ever clone a commandline?" );
/*        Commandline c = new Commandline( (Shell) shell.clone() );
       c.addArguments( getArguments() );
        return c;*/
    }

    /**
     * Sets execution directory.
     */
    public void setWorkingDirectory( String path )
    {
        shell.setWorkingDirectory( path );
    }

    /**
     * Sets execution directory.
     */
    public void setWorkingDirectory( File workingDirectory )
    {
        shell.setWorkingDirectory( workingDirectory );
    }

    public File getWorkingDirectory()
    {
        return shell.getWorkingDirectory();
    }

    /**
     * Clear out the arguments but leave the executable in place for another operation.
     */
    public void clearArgs()
    {
        arguments.clear();
    }

    /**
     * Executes the command.
     */
    public Process execute()
        throws CommandLineException
    {
        Process process;

        //addEnvironment( "MAVEN_TEST_ENVAR", "MAVEN_TEST_ENVAR_VALUE" );

        String[] environment = getEnvironmentVariables();

        File workingDir = shell.getWorkingDirectory();

        try
        {
            if ( workingDir == null )
            {
                process = Runtime.getRuntime().exec( getShellCommandline(), environment );
            }
            else
            {
                if ( !workingDir.exists() )
                {
                    throw new CommandLineException(
                        "Working directory \"" + workingDir.getPath() + "\" does not exist!" );
                }
                else if ( !workingDir.isDirectory() )
                {
                    throw new CommandLineException(
                        "Path \"" + workingDir.getPath() + "\" does not specify a directory." );
                }

                process = Runtime.getRuntime().exec( getShellCommandline(), environment, workingDir );
            }
        }
        catch ( IOException ex )
        {
            throw new CommandLineException( "Error while executing process.", ex );
        }

        return process;
    }

    /**
     * Allows to set the shell to be used in this command line.
     *
     * @param shell the shell
     * 
     */
    void setShell( Shell shell )
    {
        this.shell = shell;
    }

    /**
     * Get the shell to be used in this command line.
     *
     * 
     */
    public Shell getShell()
    {
        return shell;
    }

    public static class Argument
        implements Arg
    {
        private String[] parts;

        /* (non-Javadoc)
         * @see org.apache.maven.shared.utils.cli.Argumnt#setValue(java.lang.String)
         */
        public void setValue( String value )
        {
            if ( value != null )
            {
                parts = new String[]{ value };
            }
        }

        /* (non-Javadoc)
         * @see org.apache.maven.shared.utils.cli.Argumnt#setLine(java.lang.String)
         */
        public void setLine( String line )
        {
            if ( line == null )
            {
                return;
            }
            try
            {
                parts = CommandLineUtils.translateCommandline( line );
            }
            catch ( Exception e )
            {
                System.err.println( "Error translating Commandline." );
            }
        }

        /* (non-Javadoc)
         * @see org.apache.maven.shared.utils.cli.Argumnt#setFile(java.io.File)
         */
        public void setFile( File value )
        {
            parts = new String[]{ value.getAbsolutePath() };
        }

        /* (non-Javadoc)
         * @see org.apache.maven.shared.utils.cli.Argumnt#getParts()
         */
        private String[] getParts()
        {
            return parts;
        }
    }
}
