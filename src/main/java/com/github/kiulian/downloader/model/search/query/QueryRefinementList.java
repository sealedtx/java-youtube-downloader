package com.github.kiulian.downloader.model.search.query;

import java.util.ArrayList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@SuppressWarnings("serial")
public class QueryRefinementList extends ArrayList<QueryRefinement> implements QueryElement {

    private final String title;

    public QueryRefinementList(JSONObject json) {
        super(json.getJSONArray("cards").size());
        title = json.getJSONObject("header")
                .getJSONObject("richListHeaderRenderer")
                .getJSONObject("title")
                .getString("simpleText");
        JSONArray jsonCards = json.getJSONArray("cards");
        for (int i = 0; i < jsonCards.size(); i++) {
            JSONObject jsonRenderer = jsonCards.getJSONObject(i).getJSONObject("searchRefinementCardRenderer"); 
            add(new QueryRefinement(jsonRenderer));
        }
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public QueryElementType type() {
        return QueryElementType.REFINEMENT_LIST;
    }
}
