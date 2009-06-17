/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package plugins.Interdex.index;

import freenet.keys.FreenetURI;

import plugins.Interdex.util.PrefixTreeMap.PrefixKey;
import plugins.Interdex.util.PrefixTreeMap.AbstractPrefixKey;

/**
** @author infinity0
** @see PrefixTreeMap.PrefixKey
*/
public class URIKey extends AbstractPrefixKey implements PrefixKey {

	// TODO
	final FreenetURI uri;

	public URIKey() {
		uri = null;
	}
	public URIKey(FreenetURI u) {
		uri = u;
	}

	public String toString() { return uri.toString(); }


	/************************************************************************
	 * public interface PrefixTreeMap.PrefixKey
	 ************************************************************************/

	public Object clone() { throw new UnsupportedOperationException("Not implemented."); }

	public int symbols() { throw new UnsupportedOperationException("Not implemented."); }

	public int size() { throw new UnsupportedOperationException("Not implemented."); }

	public int get(int i) { throw new UnsupportedOperationException("Not implemented."); }

	public void set(int i, int v) { throw new UnsupportedOperationException("Not implemented."); }

	public void clear(int i) { throw new UnsupportedOperationException("Not implemented."); }


}