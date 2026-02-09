import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, FormControl, InputLabel, Select,
  MenuItem, Button, Grid, CircularProgress, Alert, Divider
} from '@mui/material';
import {
  ArrowBack as BackIcon, Save as SaveIcon
} from '@mui/icons-material';
import { serversAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

const SERVER_TYPES = [
  { value: 'SERVER', label: 'Serveur' },
  { value: 'NAS', label: 'NAS' },
  { value: 'DBAAS', label: 'Base de donnees DBaaS' }
];

const INFRASTRUCTURE_TYPES = [
  { value: 'PHYSICAL', label: 'Physique' },
  { value: 'VIRTUAL', label: 'Virtuel (VM)' }
];

const DEFAULT_ENVIRONMENTS = ['PROD', 'PREPROD', 'RECETTE', 'DEV', 'TEST'];

function ServerForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const isEdit = Boolean(id);

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const [environments, setEnvironments] = useState(DEFAULT_ENVIRONMENTS);
  const [sites, setSites] = useState([]);

  const [formData, setFormData] = useState({
    resourceName: '',
    hostname: '',
    ipFront: '',
    ipAdmin: '',
    ipIlo: '',
    refDat: '',
    serverType: 'SERVER',
    infrastructureType: '',
    environmentName: '',
    siteCode: '',
    dataCenter: '',
    operatingSystem: '',
    osVersion: '',
    dbInstance: '',
    dbVersion: '',
    dbType: '',
    notes: ''
  });

  useEffect(() => {
    loadFilters();
    if (isEdit) {
      loadServer();
    }
  }, [id]);

  const loadFilters = async () => {
    try {
      const [envRes, siteRes] = await Promise.all([
        serversAPI.getEnvironments(),
        serversAPI.getSites()
      ]);
      // Combiner les environnements existants avec les defauts
      const existingEnvs = envRes.data || [];
      const allEnvs = [...new Set([...DEFAULT_ENVIRONMENTS, ...existingEnvs])];
      setEnvironments(allEnvs);
      setSites(siteRes.data || []);
    } catch (err) {
      console.error('Erreur chargement filtres:', err);
    }
  };

  const loadServer = async () => {
    try {
      setLoading(true);
      const response = await serversAPI.getById(id);
      const server = response.data;
      setFormData({
        resourceName: server.resourceName || '',
        hostname: server.hostname || '',
        ipFront: server.ipFront || '',
        ipAdmin: server.ipAdmin || '',
        ipIlo: server.ipIlo || '',
        refDat: server.refDat || '',
        serverType: server.serverType || 'SERVER',
        infrastructureType: server.infrastructureType || '',
        environmentName: server.environmentName || '',
        siteCode: server.siteCode || '',
        dataCenter: server.dataCenter || '',
        operatingSystem: server.os || '',
        osVersion: server.osVersion || '',
        dbInstance: server.dbInstance || '',
        dbVersion: server.dbVersion || '',
        dbType: server.dbType || '',
        notes: server.notes || ''
      });
    } catch (err) {
      console.error('Erreur chargement serveur:', err);
      setError('Serveur non trouve');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => {
      const updated = { ...prev, [name]: value };

      // Si on modifie le hostname et que resourceName est vide ou etait auto-genere
      if (name === 'hostname' && value) {
        // Auto-generer le resourceName a partir du hostname si vide
        if (!prev.resourceName || prev.resourceName === generateResourceName(prev.hostname)) {
          updated.resourceName = generateResourceName(value);
        }
      }

      return updated;
    });
  };

  // Genere un nom de ressource a partir du hostname
  const generateResourceName = (hostname) => {
    if (!hostname) return '';
    // Extraire la premiere partie du hostname (avant le premier point)
    const parts = hostname.split('.');
    return parts[0].toUpperCase();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    // Validation minimale - au moins hostname ou resourceName
    if (!formData.hostname && !formData.resourceName) {
      setError('Le hostname ou le nom de ressource est obligatoire');
      return;
    }

    // Si resourceName vide, utiliser le hostname
    const dataToSend = { ...formData };
    if (!dataToSend.resourceName && dataToSend.hostname) {
      dataToSend.resourceName = generateResourceName(dataToSend.hostname);
    }

    try {
      setSaving(true);
      if (isEdit) {
        await serversAPI.update(id, dataToSend);
      } else {
        await serversAPI.create(dataToSend);
      }
      setSuccess(true);
      setTimeout(() => {
        navigate('/servers');
      }, 1000);
    } catch (err) {
      console.error('Erreur sauvegarde:', err);
      setError(err.response?.data?.message || 'Erreur lors de la sauvegarde');
    } finally {
      setSaving(false);
    }
  };

  if (!isAuthenticated()) {
    return (
      <Box>
        <Alert severity="warning">Vous devez etre connecte pour acceder a cette page</Alert>
      </Box>
    );
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/servers')}>
          Retour
        </Button>
        <Typography variant="h5">
          {isEdit ? 'Modifier le serveur' : 'Ajouter un serveur'}
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }}>
          Serveur {isEdit ? 'modifie' : 'cree'} avec succes !
        </Alert>
      )}

      <Paper sx={{ p: 3 }}>
        <form onSubmit={handleSubmit}>
          {/* Identification */}
          <Typography variant="h6" gutterBottom>Identification</Typography>
          <Divider sx={{ mb: 2 }} />
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                required
                label="Hostname"
                name="hostname"
                value={formData.hostname}
                onChange={handleChange}
                placeholder="Ex: serveur.domain.edf.fr"
                helperText="Le nom de ressource sera genere automatiquement si non renseigne"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Nom de ressource"
                name="resourceName"
                value={formData.resourceName}
                onChange={handleChange}
                placeholder="Auto-genere depuis hostname"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Reference DAT"
                name="refDat"
                value={formData.refDat}
                onChange={handleChange}
                placeholder="Ex: Central_GED_PEI_8"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <FormControl fullWidth>
                <InputLabel>Type</InputLabel>
                <Select
                  name="serverType"
                  value={formData.serverType}
                  onChange={handleChange}
                  label="Type"
                >
                  {SERVER_TYPES.map(t => (
                    <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={4}>
              <FormControl fullWidth>
                <InputLabel>Infrastructure</InputLabel>
                <Select
                  name="infrastructureType"
                  value={formData.infrastructureType}
                  onChange={handleChange}
                  label="Infrastructure"
                >
                  <MenuItem value="">-- Selectionner --</MenuItem>
                  {INFRASTRUCTURE_TYPES.map(t => (
                    <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={4}>
              <FormControl fullWidth>
                <InputLabel>Environnement</InputLabel>
                <Select
                  name="environmentName"
                  value={formData.environmentName}
                  onChange={handleChange}
                  label="Environnement"
                >
                  <MenuItem value="">-- Selectionner --</MenuItem>
                  {environments.map(env => (
                    <MenuItem key={env} value={env}>{env}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>

          {/* Reseau */}
          <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>Reseau</Typography>
          <Divider sx={{ mb: 2 }} />
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="IP Front"
                name="ipFront"
                value={formData.ipFront}
                onChange={handleChange}
                placeholder="Ex: 10.30.108.66"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="IP Admin"
                name="ipAdmin"
                value={formData.ipAdmin}
                onChange={handleChange}
                placeholder="Ex: 10.30.108.66"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="IP ILO/IDRAC"
                name="ipIlo"
                value={formData.ipIlo}
                onChange={handleChange}
                placeholder="Ex: 10.30.108.67"
              />
            </Grid>
          </Grid>

          {/* Localisation */}
          <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>Localisation</Typography>
          <Divider sx={{ mb: 2 }} />
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Site PEI</InputLabel>
                <Select
                  name="siteCode"
                  value={formData.siteCode}
                  onChange={handleChange}
                  label="Site PEI"
                >
                  <MenuItem value="">-- Selectionner --</MenuItem>
                  {sites.map(s => (
                    <MenuItem key={s} value={s}>{s}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Data Center"
                name="dataCenter"
                value={formData.dataCenter}
                onChange={handleChange}
                placeholder="Ex: PACY, NOE"
              />
            </Grid>
          </Grid>

          {/* Systeme */}
          <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>Systeme</Typography>
          <Divider sx={{ mb: 2 }} />
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Systeme d'exploitation"
                name="operatingSystem"
                value={formData.operatingSystem}
                onChange={handleChange}
                placeholder="Ex: RHEL, Windows Server"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Version OS"
                name="osVersion"
                value={formData.osVersion}
                onChange={handleChange}
                placeholder="Ex: 8.6, 2019"
              />
            </Grid>
          </Grid>

          {/* DBaaS */}
          {formData.serverType === 'DBAAS' && (
            <>
              <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>Base de donnees</Typography>
              <Divider sx={{ mb: 2 }} />
              <Grid container spacing={2}>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Instance"
                    name="dbInstance"
                    value={formData.dbInstance}
                    onChange={handleChange}
                    placeholder="Ex: B6APROD"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Version"
                    name="dbVersion"
                    value={formData.dbVersion}
                    onChange={handleChange}
                    placeholder="Ex: 19c"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Type de base"
                    name="dbType"
                    value={formData.dbType}
                    onChange={handleChange}
                    placeholder="Ex: Oracle, PostgreSQL"
                  />
                </Grid>
              </Grid>
            </>
          )}

          {/* Notes */}
          <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>Notes</Typography>
          <Divider sx={{ mb: 2 }} />
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={4}
                label="Notes"
                name="notes"
                value={formData.notes}
                onChange={handleChange}
                placeholder="Informations complementaires..."
              />
            </Grid>
          </Grid>

          {/* Actions */}
          <Box display="flex" justifyContent="flex-end" gap={2} mt={4}>
            <Button
              variant="outlined"
              onClick={() => navigate('/servers')}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              variant="contained"
              startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}
              disabled={saving}
            >
              {saving ? 'Enregistrement...' : 'Enregistrer'}
            </Button>
          </Box>
        </form>
      </Paper>
    </Box>
  );
}

export default ServerForm;
