/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
                ArrayList<String> list = new ArrayList<>(entries);
                results.values = list;
                results.count = list.size();
                Log.i(TAG, "clearing filter with size: " + list.size());
            }else{
                final ArrayList<String> list = new ArrayList<>(entries);
                final ArrayList<String> nlist = new ArrayList<>();
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