/**
 * 
 */
package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pl.electoroffline.DrawerMenuGroup.DrawerMenuItem;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Michał Ziobro
 *
 */
public class DrawerMenuAdapter extends BaseExpandableListAdapter {
	
	private final SparseArray<DrawerMenuGroup> groups;
	private LayoutInflater inflater; 
	private Activity activity; 
	
	public DrawerMenuAdapter(Activity a, int xmlDrawerMenuRes) {
		activity = a; 
		groups = parseXMLDrawerMenuResource(xmlDrawerMenuRes);
		inflater = a.getLayoutInflater(); 
		
		/*for(int i=0; i< groups.size(); i++) { 
			Log.w(DrawerMenuAdapter.class.getName(), groups.get(i).items().toString()); 
		}*/
	}
	
	public DrawerMenuAdapter(Activity a, SparseArray<DrawerMenuGroup> groups) {
		activity = a; 
		this.groups = groups; 
		inflater = a.getLayoutInflater(); 
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChild(int, int)
	 */
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		
		return groups.get(groupPosition).items().get(childPosition);
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChildId(int, int)
	 */
	@Override
	public long getChildId(int groupPosition, int childPosition) {
		
		return ((DrawerMenuItem) groups.get(groupPosition).items().get(childPosition)).id();
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChildView(int, int, boolean, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		
		final DrawerMenuItem menuItem = (DrawerMenuItem) getChild(groupPosition, childPosition); 
		
		TextView titleTextView = null; 
		TextView subtitleTextView = null; 
		ImageView iconView = null; 
		if(convertView == null) { 
			convertView = inflater.inflate(R.layout.drawerlist_item_row, null);
		}
		titleTextView = (TextView) convertView.findViewById(R.id.drawerlist_item_title);
		subtitleTextView = (TextView) convertView.findViewById(R.id.drawerlist_item_subtitle);
		iconView = (ImageView) convertView.findViewById(R.id.drawerlist_item_icon); 
		
		titleTextView.setText(menuItem.title());
		subtitleTextView.setText(menuItem.subtitle());
		iconView.setImageDrawable(menuItem.icon()); 
		
		if(getCurrentScreenClass().equals(menuItem.contentClass()) )
		{
			convertView.setBackgroundResource(R.drawable.drawer_item_shape_pressed2);
		} else { 
			convertView.setBackgroundResource(R.drawable.drawer_item_shape2);
		}
		
		/*convertView.setOnClickListener( new OnClickListener() {

			 @Override
			public void onClick(View v) {
				//Toast.makeText(activity, "Drawer list item: " 
				//						+ menuItem.title() + " clicked.", Toast.LENGTH_LONG).show();
				
				
			}
			
		});*/
	
		return convertView;
	}
	
	/**
	 * Helper method that returns current screen class name
	 * used to highlight suitable menu item on list view.
	 * @return
	 */
	private String getCurrentScreenClass()
	{
		String currentScreen = activity.getClass().getName();
		
		if(currentScreen.equals(MainActivity.class.getName())) { 
			Fragment fragment = ((MainActivity) activity).getSupportFragmentManager().findFragmentById(R.id.main_content_frame);
			currentScreen = fragment.getClass().getName();
		}
		Log.w(DrawerMenuAdapter.class.getName(), "Current Screen class: " + currentScreen);
		
		return currentScreen;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChildrenCount(int)
	 */
	@Override
	public int getChildrenCount(int groupPosition) {
		Log.w(DrawerMenuAdapter.class.getName(), "Number of child views: " + groups.get(groupPosition).items().size());
		return groups.get(groupPosition).items().size();
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getGroup(int)
	 */
	@Override
	public Object getGroup(int groupPosition) { 
		return groups.get(groupPosition);
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getGroupCount()
	 */
	@Override
	public int getGroupCount() {
		return groups.size();
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getGroupId(int)
	 */
	@Override
	public long getGroupId(int groupPosition) {
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getGroupView(int, boolean, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		final DrawerMenuGroup menuGroup = (DrawerMenuGroup) getGroup(groupPosition);
		
		TextView titleTextView = null; 
		TextView subtitleTextView = null; 
		ImageView iconView = null;
		if(convertView == null) {
			convertView = inflater.inflate(R.layout.drawerlist_group_row, null);
		}
		titleTextView = (TextView) convertView.findViewById(R.id.drawerlist_group_title);
		subtitleTextView = (TextView) convertView.findViewById(R.id.drawerlist_group_subtitle);
		iconView = (ImageView) convertView.findViewById(R.id.drawerlist_group_icon);
		
		titleTextView.setText(menuGroup.title());
		subtitleTextView.setText(menuGroup.subtitle());
		iconView.setImageDrawable(menuGroup.icon()); 
		
		convertView.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Toast.makeText(activity, "Drawer list group header: " 
				//						+ menuGroup.title() + " clicked.", Toast.LENGTH_LONG).show();
				
			}
			
		});
		
		return convertView;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#hasStableIds()
	 */
	@Override
	public boolean hasStableIds() {
		return false;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#isChildSelectable(int, int)
	 */
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
	
	public SparseArray<DrawerMenuGroup> parseXMLDrawerMenuResource(int xmlDrawerMenuRes) {
		
		return parseXMLDrawerMenuResource(activity, xmlDrawerMenuRes);
	}
	
	/**
	 * Helper method used to parse XML resource with DrawerMenu data
	 * This method reads content of XML file and puts data into SparseArray<DrawerMenuGroup> object
	 * @param ctx - app context 
	 * @param xmlDrawerMenuRes - integer XML resource identifier 
	 * @return
	 */
	public static SparseArray<DrawerMenuGroup> parseXMLDrawerMenuResource(Context ctx, int xmlDrawerMenuRes) {
		
		SparseArray<DrawerMenuGroup> groups = new SparseArray<DrawerMenuGroup>();
		
		InputStream is = ctx.getResources().openRawResource(xmlDrawerMenuRes);
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(is);
			
			doc.getDocumentElement().normalize();
			
			NodeList groupList = doc.getElementsByTagName("Group");
			
			for(int i=0; i < groupList.getLength(); i++) { 
				
				// access next group node 
				Node groupNode = (Node) groupList.item(i); 
				if (groupNode.getNodeType() != Node.ELEMENT_NODE) continue; 
				
				// cast Node to Element 
				Element groupElement = (Element) groupNode;
				
				Drawable groupIcon = null; 
				if(!groupElement.getAttribute("icon").equals(""))  { 
					// if group's icon attribute isn't empty string get Drawable resource 
					int iconResourceId = ctx.getResources()
											.getIdentifier(groupElement.getAttribute("icon"), 
															"drawable", ctx.getPackageName());
					groupIcon = ctx.getResources().getDrawable(iconResourceId);
				}
				// create new DrawerMenuGroup object 
				DrawerMenuGroup group = new DrawerMenuGroup( groupElement.getAttribute("title"),
														     groupElement.getAttribute("subtitle"),
														     groupIcon); 
				// pass through all items in current menu's group
				NodeList itemList = groupElement.getElementsByTagName("item");
				for(int j=0; j < itemList.getLength(); j++) { 
					
						// access next item node 
						Node itemNode = (Node) itemList.item(j); 
						if (itemNode.getNodeType() != Node.ELEMENT_NODE) continue; 
						
						// cast Node to Element 
						Element itemElement = (Element) itemNode; 
						
						Drawable itemIcon = null; 
						if(!itemElement.getAttribute("icon").equals("")) {
							// if item's icon attribute isn't empty string get Drawable resource
							int iconResourceId = ctx.getResources()
									.getIdentifier(itemElement.getAttribute("icon"), 
													"drawable", ctx.getPackageName());
							itemIcon = ctx.getResources().getDrawable(iconResourceId);
						}
						
						group.addChildItem(Integer.valueOf(itemElement.getAttribute("id")),
										   itemElement.getFirstChild().getNodeValue(), 
										   itemElement.getAttribute("subtitle"), itemIcon,
										   itemElement.getAttribute("class"));
				}
				
				groups.append(i, group);
			
			}
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return groups; 
	}

}
