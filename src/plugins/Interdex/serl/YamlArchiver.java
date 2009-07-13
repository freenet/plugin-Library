/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package plugins.Interdex.serl;

import plugins.Interdex.serl.Serialiser.*;

import freenet.keys.FreenetURI;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.Dumper;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.channels.FileLock;

/**
** Converts between a map of {@link String} to {@link Object}, and a YAML
** document. The object must be serialisable as defined in {@link Serialiser}.
**
** This class expects {@link Task#meta} to be of type {@link String}, or an
** array whose first element is of type {@link String}.
**
** @author infinity0
*/
public class YamlArchiver<T extends Map<String, Object>> implements Archiver<T> {

	/**
	** Thread local yaml processor.
	**
	** @see ThreadLocal
	*/
	final private static ThreadLocal<Yaml> yaml = new ThreadLocal() {
		protected synchronized Yaml initialValue() {
			return new Yaml(new Loader(new FreenetURIConstructor()),
			                new Dumper(new FreenetURIRepresenter(), new DumperOptions()));
		}
	};

	// DEBUG
	private static boolean testmode = false;
	public static void setTestMode() { System.out.println("YamlArchiver will now randomly pause between 1 and 5 seconds for a task"); testmode = true; }
	public static void randomWait() { try { Thread.sleep((long)(Math.random()*4+1)*1000); } catch (InterruptedException e) { } }

	/**
	** Prefix of filename
	*/
	protected final String prefix;
	/**
	** Suffix of filename
	*/
	protected final String suffix;

	public YamlArchiver() {
		suffix = prefix = "";
	}

	public YamlArchiver(String pre, String suf) {
		prefix = (pre == null)? "": pre;
		suffix = (suf == null)? "": suf;
	}

	protected String[] getFileParts(Object meta) {
		String[] m = new String[]{"", ""};
		if (meta instanceof String) {
			m[0] = (String)(meta);
		} else if (meta instanceof Object[]) {
			Object[] arr = (Object[])meta;
			if (arr.length > 0 && arr[0] instanceof String) {
				m[0] = (String)arr[0];
				if (arr.length > 1) {
					StringBuilder str = new StringBuilder(arr[1].toString());
					for (int i=2; i<arr.length; ++i) {
						str.append('.').append(arr[i].toString());
					}
					m[1] = str.toString();
				}
			} else {
				throw new IllegalArgumentException("YamlArchiver does not support such metadata: " + meta);
			}
		} else if (meta != null) {
			throw new IllegalArgumentException("YamlArchiver does not support such metadata: " + meta);
		}

		return m;
	}

	/*========================================================================
	  public interface Archiver
	 ========================================================================*/

	@Override public void pull(PullTask<T> t) {
		String[] s = getFileParts(t.meta);
		File file = new File(prefix + s[0] + suffix + s[1] + ".yml");
		try {
			FileInputStream is = new FileInputStream(file);
			try {
				if (testmode) { randomWait(); }
				FileLock lock = is.getChannel().lock(0L, Long.MAX_VALUE, true); // shared lock for reading
				try {
					t.data = (T)yaml.get().load(new InputStreamReader(is));
				} catch (YAMLException e) {
					throw new DataFormatException("Yaml could not process the document " + file, e, file, null, null);
				} finally {
					lock.release();
				}
			} finally {
				try { is.close(); } catch (IOException f) { }
			}
		} catch (IOException e) {
			throw new TaskFailException(e);
		}
	}

	@Override public void push(PushTask<T> t) {
		String[] s = getFileParts(t.meta);
		File file = new File(prefix + s[0] + suffix + s[1] + ".yml");
		try {
			FileOutputStream os = new FileOutputStream(file);
			try {
				if (testmode) { randomWait(); }
				FileLock lock = os.getChannel().lock();
				try {
					yaml.get().dump(t.data, new OutputStreamWriter(os));
				} catch (YAMLException e) {
					throw new DataFormatException("Yaml could not process the object", e, t.data, null, null);
				} finally {
					lock.release();
				}
			} finally {
				try { os.close(); } catch (IOException f) { }
			}
		} catch (java.io.IOException e) {
			throw new TaskFailException(e);
		}
	}

	public static class FreenetURIRepresenter extends Representer {
		public FreenetURIRepresenter() {
			this.representers.put(FreenetURI.class, new RepresentFreenetURI());
		}

		private class RepresentFreenetURI implements Represent {
			@Override public Node representData(Object data) {
				return representScalar("!FreenetURI", ((FreenetURI) data).toString());
			}
		}
	}

	public static class FreenetURIConstructor extends Constructor {
		public FreenetURIConstructor() {
			this.yamlConstructors.put("!FreenetURI", new ConstructFreenetURI());
		}

		private class ConstructFreenetURI implements Construct {
			@Override public Object construct(Node node) {
				String uri = (String) constructScalar((ScalarNode)node);
				try {
					return new FreenetURI(uri);
				} catch (java.net.MalformedURLException e) {
					throw new ConstructorException("while constructing a FreenetURI", node.getStartMark(), "found malformed URI " + uri, null) {};
				}
			}
			// TODO this might be removed in snakeYAML later
			@Override public void construct2ndStep(Node node, Object object) { }
		}
	}

}
