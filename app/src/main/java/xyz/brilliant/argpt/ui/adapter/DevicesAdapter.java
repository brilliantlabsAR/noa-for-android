package xyz.brilliant.argpt.ui.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.le.ScanResult;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import xyz.brilliant.argpt.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itanbarpeled on 28/01/2018.
 */

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder>  {


    public interface DevicesAdapterListener {
        void onDeviceItemClick(String deviceName, String deviceAddress);
    }

    /*
    class Temp {
        String name;
        String address;

        public Temp(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }
    */

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mDeviceNameView;
        TextView mDeviceNameAddressView;

        ViewHolder(View view) {

            super(view);
            mDeviceNameView = view.findViewById(R.id.device_name);
            mDeviceNameAddressView = view.findViewById(R.id.device_address);
        }

    }

    //private ArrayList<Temp> mArrayList;
    private final ArrayList<ScanResult> mArrayList;
    private final DevicesAdapterListener mListener;


    public DevicesAdapter(DevicesAdapterListener listener) {
        mArrayList = new ArrayList<>();
        mListener = listener;

        /*
        Temp device = new Temp("Pixel", "AA:12:AA:12:AA:12");
        mArrayList.add(device);
        device = new Temp("Galaxy S8", "BB:23:BB:23:BB:23");
        mArrayList.add(device);
        notifyDataSetChanged();
        */
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_device_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ScanResult scanResult = mArrayList.get(position);
        @SuppressLint("MissingPermission") final String deviceName = scanResult.getDevice().getName();
        final String deviceAddress = scanResult.getDevice().getAddress();

        /*
        Temp scanResult = mArrayList.get(position);
        final String deviceName = scanResult.name;
        final String deviceAddress = scanResult.address;
        */

        if (TextUtils.isEmpty(deviceName)) {
            holder.mDeviceNameView.setText("");
        } else {
            holder.mDeviceNameView.setText(deviceName);
        }

        if (TextUtils.isEmpty(deviceAddress)) {
            holder.mDeviceNameAddressView.setText("");
        } else {
            holder.mDeviceNameAddressView.setText(deviceAddress);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(deviceName) && !TextUtils.isEmpty(deviceAddress)) {
                    if (mListener != null) {
                        mListener.onDeviceItemClick(deviceName, deviceAddress);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mArrayList.size();
    }

    public void add(ScanResult scanResult) {
        add(scanResult, true);
    }

    /**
     * Add a ScanResult item to the adapter if a result from that device isn't already present.
     * Otherwise updates the existing position with the new ScanResult.
     */
    public void add(ScanResult scanResult, boolean notify) {

        if (scanResult == null) {
            return;
        }

        int existingPosition = getPosition(scanResult.getDevice().getAddress());

        if (existingPosition >= 0) {
            // Device is already in list, update its record.
            mArrayList.set(existingPosition, scanResult);
        } else {
            // Add new Device's ScanResult to list.
            mArrayList.add(scanResult);
        }

        if (notify) {
            notifyDataSetChanged();
        }

    }

    public void add(List<ScanResult> scanResults) {
        if (scanResults != null) {
            for (ScanResult scanResult : scanResults) {
                add(scanResult, false);
            }
            notifyDataSetChanged();
        }
    }


    /**
     * Search the adapter for an existing device address and return it, otherwise return -1.
     */
    private int getPosition(String address) {
        int position = -1;
        for (int i = 0; i < mArrayList.size(); i++) {
            if (mArrayList.get(i).getDevice().getAddress().equals(address)) {
                position = i;
                break;
            }
        }
        return position;
    }


}
