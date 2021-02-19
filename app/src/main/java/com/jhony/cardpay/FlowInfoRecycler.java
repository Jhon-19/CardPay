package com.jhony.cardpay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.intellij.lang.annotations.Flow;

import java.util.List;

public class FlowInfoRecycler {
    private Activity mContext;
    private List<FlowInfoItem> mFlowInfoItemList;
    private RecyclerView mFlowInfos;
    private RecyclerView.Adapter mAdapter;
    private AlertDialog.Builder mBuilder;

    private FlowInfoRecycler(Activity context, List<FlowInfoItem> flowInfoItemList) {
        mContext = context;
        mFlowInfoItemList = flowInfoItemList;
        setFlowInfos();
    }

    //构造方法
    public static void build(Activity context, List<FlowInfoItem> flowInfoItemList) {
        new FlowInfoRecycler(context, flowInfoItemList);
    }

    //初始化流水信息表
    private void setFlowInfos() {
        mBuilder = new AlertDialog.Builder(mContext);
        View view = View.inflate(mContext, R.layout.dialog_flow_info, null);

        mFlowInfos = view.findViewById(R.id.flow_infos);
        mFlowInfos.setLayoutManager(new LinearLayoutManager(mContext));
        //添加分割线
        DividerItemDecoration divider = new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL);
        divider.setDrawable(mContext.getDrawable(R.drawable.divider));
        mFlowInfos.addItemDecoration(divider);
        mAdapter = new FlowInfoAdapter(mFlowInfoItemList);
        mFlowInfos.setAdapter(mAdapter);

        mBuilder.setTitle("一周内的流水信息:")
                .setView(view);
        showFlowInfos();
    }

    //展示流水信息
    private void showFlowInfos(){
        mContext.runOnUiThread(() -> mBuilder
                .create()
                .show());
    }

    private class FlowInfoHolder extends RecyclerView.ViewHolder {
        private TextView mEffectDateText;
        private TextView mTranAmtText;
        private TextView mTranNameText;
        private TextView mCardBalText;
        private FlowInfoItem mFlowInfoItem;

        public FlowInfoHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.flow_info_item, parent, false));

            mEffectDateText = itemView.findViewById(R.id.effect_date_text);
            mTranAmtText = itemView.findViewById(R.id.tran_amt_text);
            mTranNameText = itemView.findViewById(R.id.tran_name_text);
            mCardBalText = itemView.findViewById(R.id.card_bal_text);
        }

        public void bind(FlowInfoItem flowInfoItem) {
            mFlowInfoItem = flowInfoItem;
            mEffectDateText.setText(mFlowInfoItem.getEffectDate());
            mTranAmtText.setText(mFlowInfoItem.getTranAmt());
            mTranNameText.setText(mFlowInfoItem.getTranName());
            mCardBalText.setText(mFlowInfoItem.getCardBal());
        }
    }

    private class FlowInfoAdapter extends RecyclerView.Adapter<FlowInfoHolder> {
        private List<FlowInfoItem> mFlowInfoItems;

        public FlowInfoAdapter(List<FlowInfoItem> flowInfoItems) {
            mFlowInfoItems = flowInfoItems;
        }

        @NonNull
        @Override
        public FlowInfoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            return new FlowInfoHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull FlowInfoHolder holder, int position) {
            FlowInfoItem flowInfoItem = mFlowInfoItems.get(position);
            holder.bind(flowInfoItem);
        }

        @Override
        public int getItemCount() {
            return mFlowInfoItems.size();
        }
    }
}
