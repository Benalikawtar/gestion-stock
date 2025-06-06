package com.ensah.gestiondestock.controller;

import com.ensah.gestiondestock.model.Inventaire;
import com.ensah.gestiondestock.model.LigneInventaire;
import com.ensah.gestiondestock.model.Produit;
import com.ensah.gestiondestock.repository.ProduitRepository;
import com.ensah.gestiondestock.service.EntrepotService;
import com.ensah.gestiondestock.service.InventaireService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/inventaires")
public class InventaireController {

    @Autowired private InventaireService inventaireService;
    @Autowired private EntrepotService entrepotService;
    @Autowired private ProduitRepository produitRepository;

    @GetMapping
    public String afficherListeInventaires(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateMin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateMax,
            @RequestParam(required = false) Long entrepotId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, 4, org.springframework.data.domain.Sort.by("dateInventaire").descending());
        Page<Inventaire> inventairesPage = inventaireService.searchInventairesParPeriodeEtEntrepotPaged(dateMin, dateMax, entrepotId, pageable);

        model.addAttribute("inventairesPage", inventairesPage);
        model.addAttribute("entrepots", entrepotService.getAllEntrepots());
        model.addAttribute("dateMin", dateMin);
        model.addAttribute("dateMax", dateMax);
        model.addAttribute("entrepotId", entrepotId);
        return "inventaire/list";
    }

    @GetMapping("/form")
    public String afficherEtape1(Model model) {
        model.addAttribute("entrepots", entrepotService.getAllEntrepots());
        model.addAttribute("etape", 1);
        return "inventaire/form";
    }

    @PostMapping("/etape2")
    public String passerEtape2(@RequestParam Long entrepotId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateInventaire,
                               HttpSession session,
                               Model model) {
        session.setAttribute("entrepotId", entrepotId);
        session.setAttribute("dateInventaire", dateInventaire);
        model.addAttribute("etape", 2);
        model.addAttribute("entrepots", entrepotService.getAllEntrepots());
        return "inventaire/form";
    }

    @PostMapping("/upload")
    public String importerFichierCorrige(@RequestParam("fichier") MultipartFile fichier,
                                         HttpSession session,
                                         Model model) {
        List<LigneInventaire> ecarts = new ArrayList<>();
        Long entrepotId = (Long) session.getAttribute("entrepotId");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fichier.getInputStream()))) {
            String ligne;
            boolean first = true;
            while ((ligne = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] champs = ligne.split(",");
                if (champs.length >= 5) {
                    String reference = champs[0].trim();
                    int theorique = Integer.parseInt(champs[4].trim());

                    List<Produit> produits = produitRepository.findByReference(reference);
                    Produit produit = produits.stream()
                            .filter(p -> p.getEntrepot() != null && p.getEntrepot().getId().equals(entrepotId))
                            .findFirst()
                            .orElse(null);

                    if (produit != null) {
                        int physique = produit.getQuantiteStock();
                        int ecart = physique - theorique;

                        LigneInventaire li = new LigneInventaire();
                        li.setProduit(produit);
                        li.setQuantiteTheorique(theorique);
                        li.setQuantitePhysique(physique);
                        li.setEcart(ecart);

                        ecarts.add(li);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        session.setAttribute("ecarts", ecarts);
        model.addAttribute("ecarts", ecarts);
        model.addAttribute("etape", 3);
        model.addAttribute("entrepots", entrepotService.getAllEntrepots());
        return "inventaire/form";
    }

    @PostMapping("/valider")
    public String validerInventaire(@RequestParam String effectuePar,
                                    @RequestParam String validePar,
                                    @RequestParam(required = false) String remarque,
                                    @RequestParam List<String> justifications,
                                    HttpSession session) {

        Long entrepotId = (Long) session.getAttribute("entrepotId");
        LocalDate dateInventaire = (LocalDate) session.getAttribute("dateInventaire");
        List<LigneInventaire> ecarts = (List<LigneInventaire>) session.getAttribute("ecarts");

        Inventaire inventaire = new Inventaire();
        inventaire.setEffectuePar(effectuePar);
        inventaire.setValidePar(validePar);
        inventaire.setDateInventaire(dateInventaire);
        inventaire.setEntrepot(entrepotService.getEntrepotById(entrepotId));

        StringBuilder remarques = new StringBuilder();

        for (int i = 0; i < ecarts.size(); i++) {
            LigneInventaire li = ecarts.get(i);
            li.setInventaire(inventaire);

            String justification = justifications.get(i);
            li.setJustification(justification);

            Produit produit = li.getProduit();
            produit.setQuantiteStock(li.getQuantiteTheorique());
            produitRepository.save(produit);

            if (justification != null && !justification.isBlank()) {
                if (remarques.length() > 0) remarques.append(" | ");
                remarques.append(justification.trim());
            }
        }

        inventaire.setRemarque(remarques.toString());
        inventaire.setLignes(ecarts);
        inventaireService.enregistrerInventaire(inventaire);

        session.removeAttribute("ecarts");
        session.removeAttribute("entrepotId");
        session.removeAttribute("dateInventaire");

        return "redirect:/inventaires";
    }

    @PostMapping("/generer")
    public ResponseEntity<byte[]> genererFichierStock(@RequestParam Long entrepotId) {
        List<Produit> produits = produitRepository.findByEntrepotId(entrepotId);
        StringBuilder contenu = new StringBuilder("Référence,Libellé,Type,Unité,Quantité\n");

        for (Produit p : produits) {
            contenu.append(p.getReference()).append(",")
                    .append(p.getLibelle()).append(",")
                    .append(p.getType()).append(",")
                    .append(p.getUnite()).append(",")
                    .append(p.getQuantiteStock()).append("\n");
        }

        byte[] fichier = contenu.toString().getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock.csv");

        return ResponseEntity.ok().headers(headers).body(fichier);
    }

    @GetMapping("/ecarts/{id}")
    public String afficherEcarts(@PathVariable Long id, Model model) {
        Inventaire inventaire = inventaireService.getInventaireById(id);
        if (inventaire == null) return "redirect:/inventaires";

        model.addAttribute("inventaire", inventaire);
        model.addAttribute("ecarts", inventaire.getLignes());
        return "inventaire/ecarts";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> telechargerInventaire(@PathVariable Long id) {
        Inventaire inventaire = inventaireService.getInventaireById(id);
        if (inventaire == null) return ResponseEntity.notFound().build();

        StringBuilder contenu = new StringBuilder("Référence,Libellé,Unité,Qté Avant,Qté Après,Écart,Justification\n");
        for (LigneInventaire li : inventaire.getLignes()) {
            contenu.append(li.getProduit().getReference()).append(",")
                    .append(li.getProduit().getLibelle()).append(",")
                    .append(li.getProduit().getUnite()).append(",")
                    .append(li.getQuantitePhysique()).append(",")
                    .append(li.getQuantiteTheorique()).append(",")
                    .append(li.getEcart()).append(",")
                    .append(li.getJustification() != null ? li.getJustification() : "").append("\n");
        }

        byte[] fichier = contenu.toString().getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventaire_" + id + ".csv");

        return ResponseEntity.ok().headers(headers).body(fichier);
    }
}