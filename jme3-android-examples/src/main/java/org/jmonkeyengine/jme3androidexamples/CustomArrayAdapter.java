package org.jmonkeyengine.jme3androidexamples;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class CustomArrayAdapter extends ArrayAdapter<String> {
    private static final String TAG = "CustomArrayAdapter";

    /* List of items */
    private List<String> entries;
    private Context activity;

    /* Position of selected answer */
    private int selectedPosition = -1;
    /* Background Color of selected item */
    private int selectedBackgroundColor = 0xffff00;
    /* Background Color of non selected item */
    private int nonselectedBackgroundColor = 0x000000;
    /* Background Drawable Resource ID of selected item */
    private int selectedBackgroundResource = 0;
    /* Background Drawable Resource ID of non selected items */
    private int nonselectedBackgroundResource = 0;

    /* Variables to support list filtering */
    private ArrayList<String> filteredEntries;
    private Filter filter;

    public CustomArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
        super(context, textViewResourceId, objects);
        activity = context;
        entries = new ArrayList<String>(objects);
        filteredEntries = new ArrayList<String>(objects);
        filter = new ClassNameFilter();
    }

    /** Setter for selected item position */
    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
        Log.i(TAG, "Setting position to: " + this.selectedPosition);
    }

    /** Setter for selected item background color */
    public void setSelectedBackgroundColor(int selectedBackgroundColor) {
        this.selectedBackgroundColor = selectedBackgroundColor;
    }

    /** Setter for non selected background color */
    public void setNonSelectedBackgroundColor(int nonselectedBackgroundColor) {
        this.nonselectedBackgroundColor = nonselectedBackgroundColor;
    }

    /** Setter for selected item background resource id*/
    public void setSelectedBackgroundResource(int selectedBackgroundResource) {
        this.selectedBackgroundResource = selectedBackgroundResource;
    }

    /** Setter for non selected background resource id*/
    public void setNonSelectedBackgroundResource(int nonselectedBackgroundResource) {
        this.nonselectedBackgroundResource = nonselectedBackgroundResource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView for position: " + position + " with selectedItem: " + selectedPosition);

        View v = convertView;
        ViewHolder holder;
        if (v == null) {
            LayoutInflater vi =
                    (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.test_chooser_row, null);
            holder = new ViewHolder();
            holder.textView = (TextView) v.findViewById(R.id.txtClassName);
            holder.layoutRow = (LinearLayout) v.findViewById(R.id.layoutTestChooserRow);
            v.setTag(holder);
        } else {
            holder=(ViewHolder)v.getTag();
        }

        final String itemText = filteredEntries.get(position);
        if (itemText != null) {
            holder.textView.setText(itemText);
            if (position == selectedPosition) {
                Log.i(TAG, "setting Background Color to: " + selectedBackgroundColor);
//                holder.textView.setBackgroundColor(selectedBackgroundColor);
//                holder.textView.setBackgroundResource(selectedBackgroundResource);
                holder.layoutRow.setBackgroundResource(selectedBackgroundResource);
            } else {
                Log.i(TAG, "setting Background Color to: " + nonselectedBackgroundColor);
//                holder.textView.setBackgroundColor(nonselectedBackgroundColor);
//                holder.textView.setBackgroundResource(nonselectedBackgroundResource);
                holder.layoutRow.setBackgroundResource(nonselectedBackgroundResource);
            }
        }
        return v;

    }

    @Override
    public Filter getFilter(){
        if(filter == null){
            filter = new ClassNameFilter();
        }
        return filter;
    }

    public static class ViewHolder{
        public TextView textView;
        public LinearLayout layoutRow;
    }

    private class ClassNameFilter extends Filter{
        @Override
        protected FilterResults performFiltering(CharSequence constraint){
            FilterResults results = new FilterResults();
            String prefix = constraint.toString().toLowerCase();
            Log.i(TAG, "performFiltering: entries size: " + entries.size());
            if (prefix == null || prefix.length() == 0){
                ArrayList<String> list = new ArrayList<String>(entries);
                results.values = list;
                results.count = list.size();
                Log.i(TAG, "clearing filter with size: " + list.size());
            }else{
                final ArrayList<String> list = new ArrayList<String>(entries);
                final ArrayList<String> nlist = new ArrayList<String>();
                int count = list.size();

                for (int i = 0; i<count; i++){
                    if(list.get(i).toLowerCase().contains(prefix)){
                        nlist.add(list.get(i));
                    }
                    results.values = nlist;
                    results.count = nlist.size();
                }
                Log.i(TAG, "filtered list size: " + nlist.size() + ", entries size: " + entries.size());
            }
            Log.i(TAG, "Returning filter count: " + results.count);
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredEntries = (ArrayList<String>)results.values;
            notifyDataSetChanged();
            clear();
            int count = filteredEntries.size();
            for(int i = 0; i<count; i++){
                add(filteredEntries.get(i));
                notifyDataSetInvalidated();
            }
            Log.i(TAG, "publishing results with size: " + count);
        }
    }

}