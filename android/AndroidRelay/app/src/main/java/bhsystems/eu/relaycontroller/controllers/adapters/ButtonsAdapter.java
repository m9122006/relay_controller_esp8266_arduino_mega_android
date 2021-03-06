package bhsystems.eu.relaycontroller.controllers.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import bhsystems.eu.relaycontroller.controllers.holders.BindableRelayControllerButtonViewHolder;
import bhsystems.eu.relaycontroller.controllers.holders.BindableViewHolder;
import bhsystems.eu.relaycontroller.model.RelayControllerButton;

/**
 * ButtonsAdapter
 * Created by ivooliveira on 27/03/17.
 */

public class ButtonsAdapter extends RecyclerView.Adapter<BindableViewHolder<RelayControllerButton>> {

    private final ArrayList<RelayControllerButton> relayControllerButtons;
    private final ButtonSelectedListener buttonSelectedListener;

    public ButtonsAdapter(ButtonSelectedListener buttonSelectedListener) {
        this.relayControllerButtons = new ArrayList<>();
        this.buttonSelectedListener = buttonSelectedListener;
    }


    @Override
    public BindableViewHolder<RelayControllerButton> onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        int layoutIdForListItem = BindableRelayControllerButtonViewHolder.LAYOUT_RES_ID;
        View view = inflater.inflate(layoutIdForListItem, parent, false);
        return new BindableRelayControllerButtonViewHolder(view, buttonSelectedListener);

    }

    @Override
    public void onBindViewHolder(BindableViewHolder<RelayControllerButton> holder, int position) {
        holder.bind(position, relayControllerButtons.get(position));
    }

    @Override
    public int getItemCount() {
        return relayControllerButtons.isEmpty() ? 0 : relayControllerButtons.size();
    }

    public void addAll(List<RelayControllerButton> purchaseEventTickets) {
        if (purchaseEventTickets.isEmpty()) {
            return;
        }
        int currentSize = this.relayControllerButtons.size();
        for (RelayControllerButton purchaseEventTicket : purchaseEventTickets) {
            if (!this.relayControllerButtons.contains(purchaseEventTicket)) {
                this.relayControllerButtons.add(purchaseEventTicket);
            }
        }
        try {
            notifyItemRangeInserted(currentSize, purchaseEventTickets.size());
        } catch (Exception ignored) {
        }
    }

    public interface ButtonSelectedListener {
        void onButtonClicked(RelayControllerButton relayControllerButton);
    }


    public void removeItem(RelayControllerButton relayControllerButton) {
        int i = relayControllerButtons.indexOf(relayControllerButton);
        if (i == -1) {
            return;
        }
        relayControllerButtons.remove(i);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(i);
    }
}
