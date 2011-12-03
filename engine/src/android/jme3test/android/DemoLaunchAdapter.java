package jme3test.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

/**
 * The view adapter which gets a list of LaunchEntries and displaqs them
 * @author larynx
 *
 */
public class DemoLaunchAdapter extends BaseAdapter implements OnClickListener 
{
    
    private Context context;

    private List<DemoLaunchEntry> listDemos;

    public DemoLaunchAdapter(Context context, List<DemoLaunchEntry> listDemos) {
        this.context = context;
        this.listDemos = listDemos;
    }

    public int getCount() {
        return listDemos.size();
    }

    public Object getItem(int position) {
        return listDemos.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup viewGroup) {
        DemoLaunchEntry entry = listDemos.get(position);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.demo_row, null);
        }
        TextView tvDemoName = (TextView) convertView.findViewById(R.id.tvDemoName);
        tvDemoName.setText(entry.getName());

        TextView tvDescription = (TextView) convertView.findViewById(R.id.tvDescription);
        tvDescription.setText(entry.getDescription());
        
        return convertView;
    }

    @Override
    public void onClick(View view) {
        DemoLaunchEntry entry = (DemoLaunchEntry) view.getTag();
        
        
        

    }

    private void showDialog(DemoLaunchEntry entry) {
        // Create and show your dialog
        // Depending on the Dialogs button clicks delete it or do nothing
    }

}

