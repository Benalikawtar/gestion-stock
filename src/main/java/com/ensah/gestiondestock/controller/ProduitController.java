package com.ensah.gestiondestock.controller;

import com.ensah.gestiondestock.model.Produit;
import com.ensah.gestiondestock.service.ProduitService;
import com.ensah.gestiondestock.service.EntrepotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    @Autowired
    private ProduitService produitService;

    @Autowired
    private EntrepotService entrepotService;

    // ✅ Liste avec pagination et recherche
    @GetMapping
    public String listProduits(@RequestParam(required = false) String ref,
                               @RequestParam(required = false) Long entrepotId,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {

        Page<Produit> produitsPage = produitService.getProduitsFiltresParPage(ref, entrepotId, page, 4);

        model.addAttribute("produits", produitsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", produitsPage.getTotalPages());
        model.addAttribute("produit", new Produit());
        model.addAttribute("entrepots", entrepotService.getAllEntrepots());
        model.addAttribute("ref", ref);
        model.addAttribute("entrepotId", entrepotId);

        return "produit/list";
    }

    // ✅ Affichage du formulaire d'ajout
    @GetMapping("/form")
    public String afficherFormAjout(Model model) {
        model.addAttribute("produit", new Produit());
        model.addAttribute("entrepots", entrepotService.getAllEntrepots());
        return "produit/form";
    }

    // ✅ Enregistrement produit (validation + unicité)
    @PostMapping("/save")
    public String saveProduit(@Valid @ModelAttribute Produit produit,
                              BindingResult result,
                              Model model) {

        boolean champVide = produit.getReference() == null || produit.getReference().isBlank()
                || produit.getLibelle() == null || produit.getLibelle().isBlank()
                || produit.getType() == null || produit.getType().isBlank()
                || produit.getUnite() == null || produit.getUnite().isBlank()
                || produit.getEntrepot() == null || produit.getEntrepot().getId() == null;

        if (champVide || result.hasErrors()) {
            model.addAttribute("entrepots", entrepotService.getAllEntrepots());
            model.addAttribute("erreur", "Tous les champs sont obligatoires.");
            return "produit/form";
        }

        boolean existeDeja = produitService.produitExisteDansEntrepot(
                produit.getReference(), produit.getEntrepot().getId(), produit.getId());

        if (existeDeja) {
            model.addAttribute("entrepots", entrepotService.getAllEntrepots());
            model.addAttribute("erreur", "Un produit avec cette référence existe déjà dans cet entrepôt.");
            return "produit/form";
        }

        produitService.saveOrUpdateProduit(produit);
        return "redirect:/produits";
    }

    // ✅ Edition
    @GetMapping("/edit/{id}")
    public String editProduit(@PathVariable Long id, Model model) {
        model.addAttribute("produit", produitService.getProduitById(id));
        model.addAttribute("entrepots", entrepotService.getAllEntrepots());
        return "produit/form"; // ✅ redirige vers le bon formulaire
    }

    // ✅ Suppression
    @GetMapping("/delete/{id}")
    public String deleteProduit(@PathVariable Long id, Model model) {
        try {
            produitService.deleteProduit(id);
        } catch (Exception e) {
            model.addAttribute("erreurSuppression", "Impossible de supprimer ce produit car il est utilisé ailleurs.");
            model.addAttribute("produits", produitService.getAllProduits());
            model.addAttribute("entrepots", entrepotService.getAllEntrepots());
            return "produit/list";
        }
        return "redirect:/produits";
    }

}
