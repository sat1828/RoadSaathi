import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet.heat';

interface HeatmapPoint {
  lat: number;
  lng: number;
  intensity: number;
}

interface MapMarker {
  lat: number;
  lng: number;
  popup?: string;
  id?: string;
}

interface MapViewProps {
  center?: [number, number];
  zoom?: number;
  markers?: MapMarker[];
  heatmapData?: HeatmapPoint[];
  onMarkerClick?: (id: string) => void;
  className?: string;
}

export function MapView({
  center = [20.5937, 78.9629],
  zoom = 5,
  markers = [],
  heatmapData = [],
  onMarkerClick,
  className,
}: MapViewProps) {
  const mapRef = useRef<L.Map | null>(null);
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const heatLayerRef = useRef<L.HeatLayer | null>(null);

  useEffect(() => {
    if (mapContainerRef.current && !mapRef.current) {
      const map = L.map(mapContainerRef.current, {
        center,
        zoom,
        zoomControl: true,
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 19,
      }).addTo(map);

      markersLayerRef.current = L.layerGroup().addTo(map);
      mapRef.current = map;

      map.on('resize', () => {
        map.invalidateSize();
      });
    }

    return () => {
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
      }
    };
  }, [center, zoom]);

  useEffect(() => {
    if (!mapRef.current) return;

    if (heatLayerRef.current) {
      mapRef.current.removeLayer(heatLayerRef.current);
    }

    if (heatmapData.length > 0) {
      const points: [number, number, number][] = heatmapData.map((p) => [
        p.lat,
        p.lng,
        p.intensity,
      ]);

      heatLayerRef.current = (L as any).heatLayer(points, {
        radius: 25,
        blur: 15,
        maxZoom: 17,
        max: 1.0,
        gradient: {
          0.2: '#00ff00',
          0.4: '#ffff00',
          0.6: '#ff9900',
          0.8: '#ff0000',
        },
      }).addTo(mapRef.current);
    }
  }, [heatmapData]);

  useEffect(() => {
    if (!markersLayerRef.current || !mapRef.current) return;

    markersLayerRef.current.clearLayers();

    markers.forEach((marker) => {
      const leafletMarker = L.marker([marker.lat, marker.lng]);

      if (marker.popup) {
        leafletMarker.bindPopup(marker.popup);
      }

      if (marker.id && onMarkerClick) {
        leafletMarker.on('click', () => onMarkerClick(marker.id!));
      }

      markersLayerRef.current?.addLayer(leafletMarker);
    });
  }, [markers, onMarkerClick]);

  return (
    <div ref={mapContainerRef} className={className || 'w-full h-full min-h-[400px]'} />
  );
}
