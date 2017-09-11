package com.example.mac.mapsapp

import android.app.Fragment
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.json.JSONObject
import java.util.ArrayList


/**
 * Created by Mac on 08/09/17.
 */

class ServicesList : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.empresas, container, false)

        val recyclerView: RecyclerView = rootView.findViewById<RecyclerView>(R.id.recycler)
        recyclerView.setHasFixedSize(true)

        val layout: RecyclerView.LayoutManager = LinearLayoutManager(rootView.context,
                LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layout

        var servicos: ArrayList<Servico> = ArrayList()
        if (this.arguments != null && this.arguments.getString("jsonServicos") != null) {
            val jsonServicos = this.arguments.getString("jsonServicos")
            val jsonObj = JSONObject(jsonServicos)
            val jsonArray = jsonObj.getJSONArray("prices")
            for (i in 0..jsonArray.length() - 1) {
                val obj = jsonArray.getJSONObject(i)
                val servico = Servico(obj.getString("display_name"), (obj.getInt("duration") / 60), obj.getString("estimate"))
                servicos.add(servico)
            }
        }
        recyclerView.adapter = NossoAdapter(servicos)

        return rootView
    }
}

class Servico(val tipo: String, val tempo: Int, val preco: String)

class NossoAdapter(val servicos: List<Servico>) : RecyclerView.Adapter<NossoAdapter.NossoViewHolder>() {

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): NossoAdapter.NossoViewHolder? {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_servicos, parent, false)
        val holder = NossoViewHolder(view)
        return holder
    }


    override fun onBindViewHolder(viewHolder: NossoAdapter.NossoViewHolder,
                                  position: Int) {
        val servico = servicos.get(position)
        viewHolder.tipo.setText(servico.tipo)
        viewHolder.tempo.setText(servico.tempo.toString() + " min")
        viewHolder.preco.setText(servico.preco)
    }

    override fun getItemCount(): Int {
        return servicos.size
    }

    inner class NossoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        internal val tipo: TextView
        internal val tempo: TextView
        internal val preco: TextView

        init {
            tipo = view.findViewById(R.id.txt_nome_servico)
            tempo = view.findViewById(R.id.txt_tempo_servico)
            preco = view.findViewById(R.id.txt_preco_servico)
        }

    }
}
