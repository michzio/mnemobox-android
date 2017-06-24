package pl.electoroffline;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.util.Log;

public class DrawerMenuGroup {

	private String title; 
	private String subtitle; 
	private Drawable icon; 
	private List<DrawerMenuItem> items = new ArrayList<DrawerMenuItem>();
	
	public DrawerMenuGroup(String title) {
		this(title, null, null);
	}
	
	public DrawerMenuGroup(String title, String subtitle, Drawable icon) {
		this.title = title; 
		this.subtitle = subtitle;
		this.icon = icon; 
	}
	
	public void addChildItem(DrawerMenuItem item) { 
		items.add(item); 
	}
	
	public void addChildItem(int id, String title, String subtitle, Drawable icon, String contentClass) { 
		Log.w(DrawerMenuGroup.class.getName(), "Inserting new menu item: " + title + ", " + subtitle + "," + contentClass + "." );
		items.add(new DrawerMenuItem(id, title, subtitle, icon, contentClass));
	}
	
	public String title() {
		return title; 
	}
	
	public String subtitle() { 
		return subtitle; 
	}
	
	public Drawable icon() { 
		return icon; 
	}
	
	public List<DrawerMenuItem> items() {
		return items; 
	}
	
	public class DrawerMenuItem {
		
		private int id; 
		private String title;
		private String subtitle; 
		private Drawable icon; 
		private String contentClass;
		
		DrawerMenuItem(int id, String title, String subtitle, Drawable icon, String contentClass) {
			this.title = title;
			this.subtitle = subtitle; 
			this.icon = icon; 
			this.id = id; 
			this.contentClass = contentClass;
		}
		public int id() { 
			return id; 
		}
		
		public String title() {
			return title;
		}
		
		public String subtitle() { 
			return subtitle; 
		}
		
		public Drawable icon() { 
			return icon; 
		}
		
		public String contentClass() { 
			return contentClass; 
		}
	}
}
