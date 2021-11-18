/**
    Copyright (C) 2021 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.forrestguice.suntimeswidget.alarmclock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.settings.SolarEvents;

import java.util.ArrayList;

@SuppressWarnings("Convert2Diamond")
public class AlarmEvent
{
    /**
     * AlarmEventItem
     * wraps SolarEvent or addon-alarm URI
     */
    public static class AlarmEventItem
    {
        protected SolarEvents event;
        protected String title = null, summary = null;
        protected String uri = null;
        protected boolean resolved = false;

        public AlarmEventItem( @NonNull SolarEvents event ) {
            this.event = event;
            resolved = true;
        }

        public AlarmEventItem( @NonNull String authority, @NonNull String name, @Nullable ContentResolver resolver)
        {
            event = null;
            uri = AlarmAddon.getAlarmInfoUri(authority, name);
            resolved = AlarmAddon.queryDisplayStrings(this, resolver);
        }

        public AlarmEventItem( @Nullable String eventUri, @Nullable ContentResolver resolver)
        {
            event = SolarEvents.valueOf(eventUri, null);
            if (event == null) {
                uri = eventUri;
                title = eventUri != null ? Uri.parse(eventUri).getLastPathSegment() : null;
                resolved = AlarmAddon.queryDisplayStrings(this, resolver);
            }
        }

        @NonNull
        public String getTitle() {
            return (event != null ? event.getLongDisplayString() : title);
        }

        @Nullable
        public String getSummary() {
            return (event != null ? "" : summary);
        }

        public int getIcon() {
            return (event != null ? event.getIcon() : R.attr.icActionExtension);
        }

        public String toString() {
            return getTitle();
        }

        public String getEventID() {
            return (event != null ? event.name() : uri);
        }

        @Nullable
        public SolarEvents getEvent() {
            return event;
        }

        @Nullable
        public String getUri() {
            return uri;
        }

        public boolean isResolved() {
            return resolved;
        }
    }

    /**
     * AlarmEventAdapter
     */
    public static class AlarmEventAdapter extends ArrayAdapter<AlarmEventItem>
    {
        private final Context context;
        private final ArrayList<AlarmEventItem> items;

        public AlarmEventAdapter(Context context, ArrayList<AlarmEventItem> items)
        {
            super(context, R.layout.layout_listitem_solarevent, items);
            this.context = context;
            this.items = items;
        }

        public boolean removeItem(SolarEvents event) {
            return event != null && removeItem(event.name());
        }

        public boolean removeItem(String event)
        {
            for (AlarmEventItem item : items)
            {
                String eventID = item.getEventID();
                if (eventID != null && eventID.equals(event))
                {
                    items.remove(item);
                    notifyDataSetChanged();
                    return true;
                }
            }
            return false;
        }

        public boolean containsItem(String eventID) {
            return findItemPosition(eventID) >= 0;
        }

        public int findItemPosition(String eventID)
        {
            for (int i=0; i<items.size(); i++)
            {
                AlarmEventItem item = items.get(i);
                if (item.getEventID().equals(eventID)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return itemView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return itemView(position, convertView, parent);
        }

        private View itemView(int position, View convertView, @NonNull ViewGroup parent)
        {
            AlarmEventItem item = items.get(position);
            View view = convertView;
            if (view == null)
            {
                LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.layout_listitem_solarevent, parent, false);
            }

            ImageView iconView = (ImageView) view.findViewById(android.R.id.icon1);   // retrieve icon
            int[] iconAttr = { items.get(position).getIcon() };
            TypedArray typedArray = context.obtainStyledAttributes(iconAttr);
            int def = R.drawable.ic_moon_rise;
            int iconResource = typedArray.getResourceId(0, def);
            typedArray.recycle();

            SolarEvents event = item.getEvent();                                      // apply icon
            if (event != null) {
                SolarEvents.SolarEventsAdapter.adjustIcon(iconResource, iconView, event);
            } else {
                Resources resources = context.getResources();
                int s = (int)resources.getDimension(R.dimen.sunIconLarge_width);
                int[] iconDimen = new int[] {s,s};
                adjustIcon(iconResource, iconView, iconDimen, 8);
            }

            TextView textView = (TextView) view.findViewById(android.R.id.text1);     // apply text
            textView.setText(item.getTitle());
            return view;
        }
    }

    public static void adjustIcon(int iconRes, ImageView iconView, int[] dimen, int marginDp)
    {
        Resources resources = iconView.getContext().getResources();
        ViewGroup.LayoutParams iconParams = iconView.getLayoutParams();
        iconParams.width = dimen[0];
        iconParams.height = dimen[1];

        if (iconParams instanceof ViewGroup.MarginLayoutParams)
        {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) iconParams;
            float vertMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marginDp, resources.getDisplayMetrics());
            float horizMargin = vertMargin / 2f;
            params.setMargins((int)horizMargin, (int)vertMargin, (int)horizMargin, (int)vertMargin);
        }

        iconView.setImageDrawable(null);
        iconView.setBackgroundResource(iconRes);
    }

    public static AlarmEventAdapter createAdapter(Context context)
    {
        SolarEvents.SolarEventsAdapter solarEventsAdapter = SolarEvents.createAdapter(context);
        ArrayList<AlarmEventItem> items = new ArrayList<>();
        for (SolarEvents event : solarEventsAdapter.getChoices()) {
            items.add(new AlarmEventItem(event));
        }
        return new AlarmEventAdapter(context, items);
    }
}
