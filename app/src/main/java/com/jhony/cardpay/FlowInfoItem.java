package com.jhony.cardpay;

public class FlowInfoItem {
    private String mEffectDate;
    private String mTranAmt;
    private String mTranName;
    private String mCardBal;

    public String getEffectDate() {
        return mEffectDate;
    }

    public void setEffectDate(String effectDate) {
        mEffectDate = effectDate;
    }

    public String getTranAmt() {
        return mTranAmt;
    }

    public void setTranAmt(String tranAmt) {
        mTranAmt = tranAmt;
    }

    public String getTranName() {
        return mTranName;
    }

    public void setTranName(String tranName) {
        mTranName = tranName;
    }

    public String getCardBal() {
        return mCardBal;
    }

    public void setCardBal(String cardBal) {
        mCardBal = cardBal;
    }
}
